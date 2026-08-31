package app.tuxguitar.app.view.dialog.fretboard;

import java.util.ArrayList;
import java.util.List;

import app.tuxguitar.player.base.MidiRepeatController;
import app.tuxguitar.song.models.TGBeat;
import app.tuxguitar.song.models.TGDuration;
import app.tuxguitar.song.models.TGMeasure;
import app.tuxguitar.song.models.TGMeasureHeader;
import app.tuxguitar.song.models.TGSong;
import app.tuxguitar.song.models.TGTrack;

/**
 * Expanded playback timeline matching {@link MidiRepeatController} / the MIDI sequencer.
 * Score {@code preciseStart} values ignore repeats; this maps them to the tick-expanded
 * time the player actually runs, including repeat alternatives and loop bounds.
 */
public class TGLearningPlaybackTimeline {

	public interface BeatVisitor {
		void visit(TGBeat beat, long playPreciseStart);
	}

	private static class Occurrence {
		private final int measureIndex;
		private final long scorePreciseStart;
		private final long playPreciseStart;
		private final long playPreciseLength;

		private Occurrence(int measureIndex, long scorePreciseStart, long playPreciseStart, long playPreciseLength) {
			this.measureIndex = measureIndex;
			this.scorePreciseStart = scorePreciseStart;
			this.playPreciseStart = playPreciseStart;
			this.playPreciseLength = playPreciseLength;
		}
	}

	private final List<Occurrence> occurrences;
	private TGSong song;
	private int sHeader;
	private int eHeader;
	private int headerCount;
	private int structureSignature;

	public TGLearningPlaybackTimeline() {
		this.occurrences = new ArrayList<Occurrence>();
		this.sHeader = Integer.MIN_VALUE;
		this.eHeader = Integer.MIN_VALUE;
		this.headerCount = -1;
		this.structureSignature = 0;
	}

	public boolean rebuildIfNeeded(TGSong song, int sHeader, int eHeader) {
		int headerCount = (song != null ? song.countMeasureHeaders() : 0);
		int signature = structureSignature(song);
		if (song == this.song
				&& sHeader == this.sHeader
				&& eHeader == this.eHeader
				&& headerCount == this.headerCount
				&& signature == this.structureSignature) {
			return false;
		}
		this.rebuild(song, sHeader, eHeader);
		this.structureSignature = signature;
		return true;
	}

	public void rebuild(TGSong song, int sHeader, int eHeader) {
		this.occurrences.clear();
		this.song = song;
		this.sHeader = sHeader;
		this.eHeader = eHeader;
		this.headerCount = (song != null ? song.countMeasureHeaders() : 0);
		if (song == null || this.headerCount <= 0) {
			return;
		}

		MidiRepeatController controller = new MidiRepeatController(song, sHeader, eHeader);
		while (!controller.finished()) {
			int index = controller.getIndex();
			long move = controller.getRepeatMove();
			controller.process();
			if (controller.shouldPlay()) {
				TGMeasureHeader header = song.getMeasureHeader(index);
				long scorePreciseStart = header.getPreciseStart();
				long playPreciseStart = scorePreciseStart + TGDuration.toPreciseTime(move);
				this.occurrences.add(new Occurrence(index, scorePreciseStart, playPreciseStart, header.getPreciseLength()));
			}
		}
	}

	public boolean isEmpty() {
		return this.occurrences.isEmpty();
	}

	public long getSequencePlayPreciseStart() {
		if (this.occurrences.isEmpty()) {
			return 0L;
		}
		return this.occurrences.get(0).playPreciseStart;
	}

	public long getSequencePlayPreciseEnd() {
		if (this.occurrences.isEmpty()) {
			return 0L;
		}
		Occurrence last = this.occurrences.get(this.occurrences.size() - 1);
		return last.playPreciseStart + last.playPreciseLength;
	}

	public long getSequencePlayPreciseLength() {
		long length = this.getSequencePlayPreciseEnd() - this.getSequencePlayPreciseStart();
		return (length > 0L ? length : 0L);
	}

	/**
	 * Map a score precise-time (caret / beat) to the first matching playback occurrence.
	 */
	public long toPlayPrecise(long scorePrecise) {
		if (this.occurrences.isEmpty()) {
			return scorePrecise;
		}
		for (int i = 0; i < this.occurrences.size(); i++) {
			Occurrence occ = this.occurrences.get(i);
			long scoreEnd = occ.scorePreciseStart + occ.playPreciseLength;
			if (scorePrecise >= occ.scorePreciseStart && scorePrecise < scoreEnd) {
				return occ.playPreciseStart + (scorePrecise - occ.scorePreciseStart);
			}
		}
		Occurrence first = this.occurrences.get(0);
		if (scorePrecise < first.scorePreciseStart) {
			return first.playPreciseStart - (first.scorePreciseStart - scorePrecise);
		}
		Occurrence last = this.occurrences.get(this.occurrences.size() - 1);
		return last.playPreciseStart + (scorePrecise - last.scorePreciseStart);
	}

	/**
	 * Visit beats whose playback precise-time is in {@code (fromExclusive, toInclusive]}.
	 * When {@code wrap} is true (player loop), the sequence is unfolded so look-ahead
	 * past the end yields the notes that will play after the loop restarts.
	 */
	public void forEachBeat(TGTrack track, long fromExclusive, long toInclusive, boolean wrap, BeatVisitor visitor) {
		if (track == null || visitor == null || this.occurrences.isEmpty() || toInclusive <= fromExclusive) {
			return;
		}

		long seqStart = this.getSequencePlayPreciseStart();
		long seqLen = this.getSequencePlayPreciseLength();
		long kMin = 0L;
		long kMax = 0L;
		if (wrap && seqLen > 0L) {
			if (toInclusive > seqStart) {
				kMax = (toInclusive - seqStart) / seqLen;
			}
			if (fromExclusive > seqStart) {
				kMin = (fromExclusive - seqStart) / seqLen;
			}
			if (kMin < 0L) {
				kMin = 0L;
			}
			if (kMax < kMin) {
				kMax = kMin;
			}
		}

		for (long k = kMin; k <= kMax; k++) {
			long unfold = (wrap && seqLen > 0L) ? (k * seqLen) : 0L;
			this.visitCycle(track, fromExclusive, toInclusive, unfold, visitor);
		}
	}

	private void visitCycle(TGTrack track, long fromExclusive, long toInclusive, long unfold, BeatVisitor visitor) {
		for (int i = 0; i < this.occurrences.size(); i++) {
			Occurrence occ = this.occurrences.get(i);
			long occStart = occ.playPreciseStart + unfold;
			long occEnd = occStart + occ.playPreciseLength;
			if (occEnd <= fromExclusive || occStart > toInclusive) {
				continue;
			}
			if (occ.measureIndex < 0 || occ.measureIndex >= track.countMeasures()) {
				continue;
			}
			TGMeasure measure = track.getMeasure(occ.measureIndex);
			if (measure == null) {
				continue;
			}
			long movePrecise = occ.playPreciseStart - occ.scorePreciseStart;
			List<TGBeat> beats = measure.getBeats();
			for (int b = 0; b < beats.size(); b++) {
				TGBeat beat = beats.get(b);
				long start = beatScorePreciseStart(beat) + movePrecise + unfold;
				if (start > fromExclusive && start <= toInclusive) {
					visitor.visit(beat, start);
				}
			}
		}
	}

	private static long beatScorePreciseStart(TGBeat beat) {
		Long preciseStart = beat.getPreciseStart();
		if (preciseStart != null && preciseStart.longValue() >= 0L) {
			return preciseStart.longValue();
		}
		return TGDuration.toPreciseTime(beat.getStart());
	}

	private static int structureSignature(TGSong song) {
		if (song == null) {
			return 0;
		}
		int signature = song.countMeasureHeaders();
		for (int i = 0; i < song.countMeasureHeaders(); i++) {
			TGMeasureHeader header = song.getMeasureHeader(i);
			signature = 31 * signature + (header.isRepeatOpen() ? 1 : 0);
			signature = 31 * signature + header.getRepeatClose();
			signature = 31 * signature + header.getRepeatAlternative();
			signature = 31 * signature + (int) header.getLength();
			signature = 31 * signature + (int) header.getPreciseStart();
		}
		return signature;
	}
}
