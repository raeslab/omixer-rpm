package io.omixer.rpm.output.plot;

import java.text.DecimalFormat;
import java.util.Map;

import io.omixer.utils.Constants;

/**
 * Draws an 820 x 620 SVG with a translated drawing area, axes with ticks, lines and circles, in the markup of 1.x.
 */
final class SVGDevice {

	private static final int WIDTH = 820;
	private static final int HEIGHT = 620;
	private static final int PADDING = 10;
	static final int DRAWING_AREA_WIDTH = WIDTH - PADDING * 6;
	static final int DRAWING_AREA_HEIGHT = HEIGHT - PADDING * 3;

	private static final String AXIS_STYLE = "stroke-width:2;stroke:black;";
	private static final String LINE_STYLE = "stroke-width:2;stroke:lightblue;";

	private final StringBuilder svg = new StringBuilder();

	SVGDevice() {
		svg.append("<svg xmlns=\"http://www.w3.org/2000/svg\" width=\"").append(WIDTH).append("\" height=\"")
				.append(HEIGHT).append("\" version=\"1.1\">").append(Constants.NEW_LINE);
		svg.append("<rect width=\"").append(WIDTH).append("\" height=\"").append(HEIGHT)
				.append("\" stroke=\"white\" fill=\"white\"/>");
		svg.append("<g transform=\"translate(").append(PADDING * 5).append(", ").append(PADDING).append(")\">")
				.append(Constants.NEW_LINE);
	}

	/** @param ticks x → the value written under it, in the map's iteration order */
	void xaxis(Map<Double, Double> ticks) {
		drawLine(0, DRAWING_AREA_HEIGHT, DRAWING_AREA_WIDTH, DRAWING_AREA_HEIGHT, AXIS_STYLE);
		DecimalFormat format = new DecimalFormat("#0.00");
		ticks.forEach((x, value) -> {
			drawLine(x, DRAWING_AREA_HEIGHT, x, DRAWING_AREA_HEIGHT + 5, AXIS_STYLE);
			svg.append("<text x=\"").append(x + 10).append("\" y=\"").append(DRAWING_AREA_HEIGHT + 10)
					.append("\" dy=\".32em\" text-anchor=\"end\">").append(format.format(value)).append("</text>")
					.append(Constants.NEW_LINE);
		});
	}

	/** @param ticks y → the value written next to it, in the map's iteration order */
	void yaxis(Map<Double, Double> ticks) {
		drawLine(0, 0, 0, DRAWING_AREA_HEIGHT, AXIS_STYLE);
		ticks.forEach((y, value) -> {
			drawLine(-5, y, 0, y, AXIS_STYLE);
			svg.append("<text x=\"-9\" y=\"").append(y).append("\" dy=\".32em\" text-anchor=\"end\">").append(value)
					.append("</text>").append(Constants.NEW_LINE);
		});
	}

	void drawLine(double x1, double y1, double x2, double y2) {
		drawLine(x1, y1, x2, y2, LINE_STYLE);
	}

	private void drawLine(double x1, double y1, double x2, double y2, String style) {
		svg.append("<line x1=\"").append(x1).append("\" y1=\"").append(y1).append("\" x2=\"").append(x2)
				.append("\" y2=\"").append(y2).append("\" style=\"").append(style).append("\" />")
				.append(Constants.NEW_LINE);
	}

	void drawCircle(double x, double y, double r, String fill) {
		svg.append("<circle cx=\"").append(x).append("\" cy=\"").append(y).append("\" r=\"").append(r)
				.append("\" style=\"stroke:steelblue;stroke-width:2\" fill=\"").append(fill).append("\"/>")
				.append(Constants.NEW_LINE);
	}

	@Override
	public String toString() {
		return svg + "</g></svg>";
	}
}
