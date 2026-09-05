package app.tuxguitar.graphics.control;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import app.tuxguitar.song.models.effects.TGEffectBend;
import app.tuxguitar.song.models.effects.TGEffectBend.BendPoint;

/**
 * Maps a bend's time/pitch points onto the note duration box, for tablature drawing.
 */
public final class TGBendPath {

	public static final int VALUE_FULL_TONE = 4;
	/** Horizontal pixels per dialog time unit (0..12), so timing stays readable. */
	public static final float WIDTH_PER_POSITION = 8.0f;

	private static final float ARC_EPS = 0.5f;
	private static final float MAX_CUBIC_SWEEP = (float) (Math.PI / 2.0);
	private static final float TWO_PI = (float) (2.0 * Math.PI);

	private static final String[] AMPLITUDE = {
		"", "1/4", "1/2", "3/4", "1",
		"1\u00BC", "1\u00BD", "1\u00BE", "2",
		"2\u00BC", "2\u00BD", "2\u00BE", "3"
	};

	private TGBendPath() {
	}

	public static final class Geometry {
		public float xStart;
		public float xEnd;
		public float yOpen;
		public float yFull;
		public float yLabel;
		public float scale;
	}

	public enum SegmentKind {
		HOLD,
		BEND_UP,
		RELEASE
	}

	public static final class Vertex {
		private final float x;
		private final float y;
		private final int position;
		private final int value;

		private Vertex(float x, float y, int position, int value) {
			this.x = x;
			this.y = y;
			this.position = position;
			this.value = value;
		}

		public float getX() {
			return this.x;
		}

		public float getY() {
			return this.y;
		}

		public int getPosition() {
			return this.position;
		}

		public int getValue() {
			return this.value;
		}
	}

	public static final class Cubic {
		private final float c1x;
		private final float c1y;
		private final float c2x;
		private final float c2y;
		private final float x;
		private final float y;

		private Cubic(float c1x, float c1y, float c2x, float c2y, float x, float y) {
			this.c1x = c1x;
			this.c1y = c1y;
			this.c2x = c2x;
			this.c2y = c2y;
			this.x = x;
			this.y = y;
		}

		public float getC1x() {
			return this.c1x;
		}

		public float getC1y() {
			return this.c1y;
		}

		public float getC2x() {
			return this.c2x;
		}

		public float getC2y() {
			return this.c2y;
		}

		public float getX() {
			return this.x;
		}

		public float getY() {
			return this.y;
		}
	}

	public static final class Segment {
		private final Vertex from;
		private final Vertex to;
		private final SegmentKind kind;
		private final boolean arrowAtEnd;
		private final boolean labelAtEnd;
		private final boolean preBend;
		private final List<Cubic> cubics;
		private final float centerX;
		private final float centerY;
		private final float radius;
		private final float sweep;
		private final float endTangentX;
		private final float endTangentY;

		private Segment(Vertex from, Vertex to, SegmentKind kind, boolean arrowAtEnd, boolean labelAtEnd, boolean preBend, ArcGeom arc) {
			this.from = from;
			this.to = to;
			this.kind = kind;
			this.arrowAtEnd = arrowAtEnd;
			this.labelAtEnd = labelAtEnd;
			this.preBend = preBend;
			if (arc != null) {
				this.cubics = arc.cubics;
				this.centerX = arc.cx;
				this.centerY = arc.cy;
				this.radius = arc.r;
				this.sweep = arc.sweep;
				this.endTangentX = arc.tanX;
				this.endTangentY = arc.tanY;
			} else {
				this.cubics = Collections.emptyList();
				this.centerX = 0f;
				this.centerY = 0f;
				this.radius = 0f;
				this.sweep = 0f;
				this.endTangentX = to.getX() - from.getX();
				this.endTangentY = to.getY() - from.getY();
			}
		}

		public Vertex getFrom() {
			return this.from;
		}

		public Vertex getTo() {
			return this.to;
		}

		public SegmentKind getKind() {
			return this.kind;
		}

		public boolean isArrowAtEnd() {
			return this.arrowAtEnd;
		}

		public boolean isLabelAtEnd() {
			return this.labelAtEnd;
		}

		public boolean isPreBend() {
			return this.preBend;
		}

		public boolean isArc() {
			return this.radius > 0f && !this.cubics.isEmpty();
		}

		public List<Cubic> getCubics() {
			return this.cubics;
		}

		public float getCenterX() {
			return this.centerX;
		}

		public float getCenterY() {
			return this.centerY;
		}

		public float getRadius() {
			return this.radius;
		}

		public float getEndTangentX() {
			return this.endTangentX;
		}

		public float getEndTangentY() {
			return this.endTangentY;
		}

		public Vertex getArcMidpoint() {
			if (!isArc()) {
				return new Vertex((this.from.getX() + this.to.getX()) / 2f, (this.from.getY() + this.to.getY()) / 2f, 0, 0);
			}
			float theta0 = (float) Math.atan2(this.from.getY() - this.centerY, this.from.getX() - this.centerX);
			float mid = theta0 + this.sweep / 2f;
			return new Vertex(
				this.centerX + this.radius * (float) Math.cos(mid),
				this.centerY + this.radius * (float) Math.sin(mid),
				0, 0);
		}
	}

	private static final class ArcGeom {
		private final float cx;
		private final float cy;
		private final float r;
		private final float sweep;
		private final List<Cubic> cubics;
		private final float tanX;
		private final float tanY;

		private ArcGeom(float cx, float cy, float r, float sweep, List<Cubic> cubics, float tanX, float tanY) {
			this.cx = cx;
			this.cy = cy;
			this.r = r;
			this.sweep = sweep;
			this.cubics = cubics;
			this.tanX = tanX;
			this.tanY = tanY;
		}
	}

	public static float minimumWidth(float scale) {
		return TGEffectBend.MAX_POSITION_LENGTH * WIDTH_PER_POSITION * scale;
	}

	public static String amplitudeLabel(int value) {
		if (value <= 0 || value >= AMPLITUDE.length) {
			return "";
		}
		return AMPLITUDE[value];
	}

	public static List<Segment> build(List<BendPoint> points, Geometry geometry) {
		if (points == null || points.isEmpty() || geometry == null) {
			return Collections.emptyList();
		}

		List<int[]> raw = copyAndCollapse(points);
		if (raw.get(0)[0] > 0) {
			raw.add(0, new int[] {0, raw.get(0)[1]});
		}
		int lastIndex = raw.size() - 1;
		if (raw.get(lastIndex)[0] < TGEffectBend.MAX_POSITION_LENGTH) {
			raw.add(new int[] {TGEffectBend.MAX_POSITION_LENGTH, raw.get(lastIndex)[1]});
		}

		float usableWidth = geometry.xEnd - geometry.xStart;
		float minWidth = minimumWidth(geometry.scale);
		if (usableWidth < minWidth) {
			usableWidth = minWidth;
		}

		List<Vertex> vertices = new ArrayList<Vertex>();
		for (int[] point : raw) {
			vertices.add(new Vertex(
				mapX(point[0], geometry, usableWidth),
				mapY(point[1], geometry),
				point[0],
				point[1]));
		}

		boolean preBend = vertices.get(0).getValue() > 0;
		List<Segment> segments = new ArrayList<Segment>();
		for (int i = 0; i < vertices.size() - 1; i++) {
			Vertex from = vertices.get(i);
			Vertex to = vertices.get(i + 1);
			if (from.getPosition() == to.getPosition() && from.getValue() == to.getValue()) {
				continue;
			}
			SegmentKind kind = kindOf(from.getValue(), to.getValue());
			boolean last = (i == vertices.size() - 2);
			int nextValue = last ? to.getValue() : vertices.get(i + 2).getValue();
			boolean arrowAtEnd = false;
			if (kind != SegmentKind.HOLD) {
				int nextDelta = nextValue - to.getValue();
				if (last || nextDelta == 0
						|| (kind == SegmentKind.BEND_UP && nextDelta < 0)
						|| (kind == SegmentKind.RELEASE && nextDelta > 0)) {
					arrowAtEnd = true;
				}
			}
			boolean labelAtEnd = (to.getValue() > from.getValue()) && (last || to.getValue() >= nextValue);
			ArcGeom arc = (kind == SegmentKind.HOLD) ? null : circularArc(from, to);
			segments.add(new Segment(from, to, kind, arrowAtEnd, labelAtEnd, i == 0 && preBend, arc));
		}
		return segments;
	}

	private static ArcGeom circularArc(Vertex from, Vertex to) {
		float dx = to.getX() - from.getX();
		float dyAbs = Math.abs(to.getY() - from.getY());
		if (dx < ARC_EPS || dyAbs < ARC_EPS) {
			return null;
		}
		boolean up = to.getY() < from.getY();
		float r = (dx * dx + dyAbs * dyAbs) / (2.0f * dyAbs);
		float cx = from.getX();
		float cy = up ? (from.getY() - r) : (from.getY() + r);

		float theta0 = (float) Math.atan2(from.getY() - cy, from.getX() - cx);
		float theta1 = (float) Math.atan2(to.getY() - cy, to.getX() - cx);
		float sweep = theta1 - theta0;
		if (up) {
			while (sweep > 0f) {
				sweep -= TWO_PI;
			}
			while (sweep <= -TWO_PI) {
				sweep += TWO_PI;
			}
		} else {
			while (sweep < 0f) {
				sweep += TWO_PI;
			}
			while (sweep >= TWO_PI) {
				sweep -= TWO_PI;
			}
		}

		List<Cubic> cubics = new ArrayList<Cubic>();
		appendCubics(cubics, cx, cy, r, theta0, sweep);
		if (cubics.isEmpty()) {
			return null;
		}

		float thetaEnd = theta0 + sweep;
		float dir = (sweep >= 0f) ? 1f : -1f;
		float tanX = dir * (-(float) Math.sin(thetaEnd));
		float tanY = dir * ((float) Math.cos(thetaEnd));
		return new ArcGeom(cx, cy, r, sweep, cubics, tanX, tanY);
	}

	private static void appendCubics(List<Cubic> cubics, float cx, float cy, float r, float theta0, float sweep) {
		if (Math.abs(sweep) > MAX_CUBIC_SWEEP + 1.0e-4f) {
			float half = sweep / 2.0f;
			appendCubics(cubics, cx, cy, r, theta0, half);
			appendCubics(cubics, cx, cy, r, theta0 + half, sweep - half);
			return;
		}
		if (Math.abs(sweep) < 1.0e-6f) {
			return;
		}
		float k = (4.0f / 3.0f) * (float) Math.tan(sweep / 4.0);
		float cos0 = (float) Math.cos(theta0);
		float sin0 = (float) Math.sin(theta0);
		float theta1 = theta0 + sweep;
		float cos1 = (float) Math.cos(theta1);
		float sin1 = (float) Math.sin(theta1);
		float p0x = cx + r * cos0;
		float p0y = cy + r * sin0;
		float p3x = cx + r * cos1;
		float p3y = cy + r * sin1;
		cubics.add(new Cubic(
			p0x + k * r * (-sin0),
			p0y + k * r * cos0,
			p3x - k * r * (-sin1),
			p3y - k * r * cos1,
			p3x,
			p3y));
	}

	private static List<int[]> copyAndCollapse(List<BendPoint> points) {
		List<int[]> raw = new ArrayList<int[]>();
		for (BendPoint point : points) {
			int position = clamp(point.getPosition(), 0, TGEffectBend.MAX_POSITION_LENGTH);
			int value = clamp(point.getValue(), 0, TGEffectBend.MAX_VALUE_LENGTH);
			raw.add(new int[] {position, value});
		}
		Collections.sort(raw, new Comparator<int[]>() {
			public int compare(int[] a, int[] b) {
				return Integer.compare(a[0], b[0]);
			}
		});
		List<int[]> collapsed = new ArrayList<int[]>();
		for (int[] point : raw) {
			if (!collapsed.isEmpty() && collapsed.get(collapsed.size() - 1)[0] == point[0]) {
				collapsed.get(collapsed.size() - 1)[1] = point[1];
			} else {
				collapsed.add(point);
			}
		}
		return collapsed;
	}

	private static SegmentKind kindOf(int fromValue, int toValue) {
		if (toValue > fromValue) {
			return SegmentKind.BEND_UP;
		}
		if (toValue < fromValue) {
			return SegmentKind.RELEASE;
		}
		return SegmentKind.HOLD;
	}

	private static float mapX(int position, Geometry geometry, float usableWidth) {
		return geometry.xStart + (position / (float) TGEffectBend.MAX_POSITION_LENGTH) * usableWidth;
	}

	private static float mapY(int value, Geometry geometry) {
		float span = geometry.yOpen - geometry.yFull;
		if (span <= 0) {
			return geometry.yOpen;
		}
		float t = value / (float) VALUE_FULL_TONE;
		if (t < 0f) {
			t = 0f;
		}
		if (t > 1f) {
			t = 1f;
		}
		return geometry.yOpen - t * span;
	}

	private static int clamp(int value, int min, int max) {
		if (value < min) {
			return min;
		}
		if (value > max) {
			return max;
		}
		return value;
	}
}
