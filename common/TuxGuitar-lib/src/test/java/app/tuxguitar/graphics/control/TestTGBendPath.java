package app.tuxguitar.graphics.control;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

import app.tuxguitar.graphics.control.TGBendPath.Segment;
import app.tuxguitar.graphics.control.TGBendPath.SegmentKind;
import app.tuxguitar.song.factory.TGFactory;
import app.tuxguitar.song.models.effects.TGEffectBend;

public class TestTGBendPath {

	private static final float EPS = 0.01f;

	private final TGFactory factory = new TGFactory();

	private TGBendPath.Geometry geometry() {
		TGBendPath.Geometry geometry = new TGBendPath.Geometry();
		geometry.xStart = 100f;
		geometry.xEnd = 220f;
		geometry.yOpen = 80f;
		geometry.yFull = 20f;
		geometry.yLabel = 12f;
		geometry.scale = 1f;
		return geometry;
	}

	private TGEffectBend bend(int... positionValuePairs) {
		TGEffectBend bend = this.factory.newEffectBend();
		for (int i = 0; i < positionValuePairs.length; i += 2) {
			bend.addPoint(positionValuePairs[i], positionValuePairs[i + 1]);
		}
		return bend;
	}

	@Test
	public void testPresetBendRisesThenHolds() {
		List<Segment> segments = TGBendPath.build(bend(0, 0, 6, 4, 12, 4).getPoints(), geometry());

		assertEquals(2, segments.size());
		assertEquals(SegmentKind.BEND_UP, segments.get(0).getKind());
		assertEquals(SegmentKind.HOLD, segments.get(1).getKind());
		assertFalse(segments.get(0).isPreBend());
		assertEquals(160f, segments.get(0).getTo().getX(), EPS);
		assertEquals(60f, segments.get(1).getFrom().getY(), EPS);
		assertEquals(60f, segments.get(1).getTo().getY(), EPS);
		assertTrue(segments.get(0).isArrowAtEnd());
		assertTrue(segments.get(0).isLabelAtEnd());
	}

	@Test
	public void testDelayedBendHoldsThenRises() {
		List<Segment> segments = TGBendPath.build(bend(0, 0, 9, 0, 12, 4).getPoints(), geometry());

		assertEquals(2, segments.size());
		assertEquals(SegmentKind.HOLD, segments.get(0).getKind());
		assertEquals(190f, segments.get(0).getTo().getX(), EPS);
		assertEquals(SegmentKind.BEND_UP, segments.get(1).getKind());
		assertTrue(segments.get(1).isArrowAtEnd());
		assertTrue(segments.get(1).isLabelAtEnd());
	}

	@Test
	public void testPreBendHold() {
		List<Segment> segments = TGBendPath.build(bend(0, 4, 12, 4).getPoints(), geometry());

		assertEquals(1, segments.size());
		assertEquals(SegmentKind.HOLD, segments.get(0).getKind());
		assertTrue(segments.get(0).isPreBend());
		assertEquals(60f, segments.get(0).getFrom().getY(), EPS);
		assertEquals(60f, segments.get(0).getTo().getY(), EPS);
		assertFalse(segments.get(0).isLabelAtEnd());
	}

	@Test
	public void testSinglePointSpansFullDuration() {
		List<Segment> segments = TGBendPath.build(bend(6, 4).getPoints(), geometry());

		assertEquals(2, segments.size());
		assertTrue(segments.get(0).isPreBend());
		assertEquals(SegmentKind.HOLD, segments.get(0).getKind());
		assertEquals(SegmentKind.HOLD, segments.get(1).getKind());
		assertEquals(100f, segments.get(0).getFrom().getX(), EPS);
		assertEquals(220f, segments.get(1).getTo().getX(), EPS);
	}

	@Test
	public void testCoordinatesFollowDialogGrid() {
		List<Segment> segments = TGBendPath.build(bend(0, 0, 3, 6, 12, 6).getPoints(), geometry());

		assertEquals(2, segments.size());
		assertEquals(130f, segments.get(0).getTo().getX(), EPS);
		assertEquals(50f, segments.get(0).getTo().getY(), EPS);
		assertEquals(220f, segments.get(1).getTo().getX(), EPS);
		assertEquals(50f, segments.get(1).getTo().getY(), EPS);
	}

	@Test
	public void testMinimumWidthCoversDialogPositions() {
		assertEquals(12 * 8.0f, TGBendPath.minimumWidth(1f), EPS);
		assertEquals(12 * 16.0f, TGBendPath.minimumWidth(2f), EPS);
	}

	@Test
	public void testAmplitudeLabels() {
		assertEquals("1", TGBendPath.amplitudeLabel(4));
		assertEquals("1/2", TGBendPath.amplitudeLabel(2));
		assertEquals("1/4", TGBendPath.amplitudeLabel(1));
		assertEquals("", TGBendPath.amplitudeLabel(0));
		assertEquals("", TGBendPath.amplitudeLabel(13));
	}
}
