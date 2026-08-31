package app.tuxguitar.player.base;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import app.tuxguitar.io.tg.TestFileFormat20;
import app.tuxguitar.song.models.TGMeasureHeader;
import app.tuxguitar.song.models.TGSong;
import org.junit.jupiter.api.Test;

/**
 * Contract the learning-mode fretboard depends on: a look-ahead window must
 * follow the expanded playback sequence (repeats / alternatives / loop bounds),
 * not linear score time.
 */
public class TestRepeatLookAhead {

	private static class Occurrence {
		private final int measureNumber;
		private final long playStart;
		private final long playLength;

		private Occurrence(int measureNumber, long playStart, long playLength) {
			this.measureNumber = measureNumber;
			this.playStart = playStart;
			this.playLength = playLength;
		}
	}

	private final TGSong song;
	private final List<Occurrence> fullSong;

	public TestRepeatLookAhead() throws IOException {
		this.song = new TestFileFormat20().readSong("altRepeatLoop_20.tg", true).getSong();
		this.fullSong = expand(this.song, -1, -1);
	}

	@Test
	public void lookAheadAtRepeatCloseSeesRepeatedStartNotNextScoreMeasure() {
		// Sequence starts 1,2,3, 1, 4,... — after m3 the player repeats m1, it does not go to m4.
		int thirdIndex = indexOfMeasurePass(this.fullSong, 3, 0);
		Occurrence third = this.fullSong.get(thirdIndex);
		long from = third.playStart + third.playLength - 1;
		long to = from + third.playLength;

		List<Integer> ahead = measureNumbersInWindow(this.fullSong, from, to);
		assertFalse(ahead.isEmpty(), "look-ahead after m3 should not be empty");
		assertEquals(Integer.valueOf(1), ahead.get(0), "next sounding measure after m3 is the repeat of m1");
		assertFalse(ahead.contains(Integer.valueOf(4)), "m4 must not appear until the repeat of 1-3 is done");
	}

	@Test
	public void lookAheadWrapsWhenLooping() {
		List<Occurrence> loop123 = expand(this.song, 1, 3);
		assertFalse(loop123.isEmpty());
		Occurrence last = loop123.get(loop123.size() - 1);
		long seqStart = loop123.get(0).playStart;
		long seqEnd = last.playStart + last.playLength;
		long seqLen = seqEnd - seqStart;

		long from = seqEnd - 1;
		long to = seqEnd + last.playLength;
		List<Integer> ahead = measureNumbersInWindowUnfolded(loop123, from, to, seqStart, seqLen);
		assertFalse(ahead.isEmpty());
		assertEquals(Integer.valueOf(1), ahead.get(0), "loop wrap must show m1, not m4");
	}

	@Test
	public void alternativeEndingsDifferAcrossPasses() {
		// After the first 1,2,3,1 the sequence is 4,5 then later 4,5 again then 6,7.
		int firstM4 = indexOfMeasurePass(this.fullSong, 4, 0);
		int secondM4 = indexOfMeasurePass(this.fullSong, 4, 1);
		assertTrue(firstM4 >= 0 && secondM4 > firstM4);

		List<Integer> afterFirstPass = new ArrayList<Integer>();
		for (int i = firstM4; i < secondM4; i++) {
			afterFirstPass.add(Integer.valueOf(this.fullSong.get(i).measureNumber));
		}
		assertTrue(afterFirstPass.contains(Integer.valueOf(5)));
		assertFalse(afterFirstPass.contains(Integer.valueOf(6)), "2nd ending (m6) must not play on the first pass");
	}

	private static List<Occurrence> expand(TGSong song, int sHeader, int eHeader) {
		List<Occurrence> occurrences = new ArrayList<Occurrence>();
		MidiRepeatController controller = new MidiRepeatController(song, sHeader, eHeader);
		while (!controller.finished()) {
			int index = controller.getIndex();
			long move = controller.getRepeatMove();
			controller.process();
			if (controller.shouldPlay()) {
				TGMeasureHeader header = song.getMeasureHeader(index);
				occurrences.add(new Occurrence(header.getNumber(), header.getStart() + move, header.getLength()));
			}
		}
		return occurrences;
	}

	private static int indexOfMeasurePass(List<Occurrence> occurrences, int measureNumber, int pass) {
		int seen = 0;
		for (int i = 0; i < occurrences.size(); i++) {
			if (occurrences.get(i).measureNumber == measureNumber) {
				if (seen == pass) {
					return i;
				}
				seen++;
			}
		}
		return -1;
	}

	private static List<Integer> measureNumbersInWindow(List<Occurrence> occurrences, long fromExclusive, long toInclusive) {
		List<Integer> numbers = new ArrayList<Integer>();
		for (int i = 0; i < occurrences.size(); i++) {
			Occurrence occ = occurrences.get(i);
			long start = occ.playStart;
			if (start > fromExclusive && start <= toInclusive) {
				numbers.add(Integer.valueOf(occ.measureNumber));
			}
		}
		return numbers;
	}

	private static List<Integer> measureNumbersInWindowUnfolded(List<Occurrence> occurrences, long fromExclusive, long toInclusive, long seqStart, long seqLen) {
		List<Integer> numbers = new ArrayList<Integer>();
		long kMax = (seqLen > 0L && toInclusive > seqStart) ? (toInclusive - seqStart) / seqLen : 0L;
		for (long k = 0L; k <= kMax; k++) {
			long unfold = k * seqLen;
			for (int i = 0; i < occurrences.size(); i++) {
				Occurrence occ = occurrences.get(i);
				long start = occ.playStart + unfold;
				if (start > fromExclusive && start <= toInclusive) {
					numbers.add(Integer.valueOf(occ.measureNumber));
				}
			}
		}
		return numbers;
	}
}
