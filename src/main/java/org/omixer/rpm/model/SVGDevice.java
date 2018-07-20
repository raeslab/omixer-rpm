package org.omixer.rpm.model;

import java.text.DecimalFormat;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import org.omixer.utils.Constants;

public class SVGDevice {

	private String svg = null;
	private int padding = 10;
	private int width = 820;
	private int height = 620;
	private int drawingAreaWidth = width - (padding * 6);
	private int drawingAreaHeight = height - (padding * 3);

	public SVGDevice(int width, int height) {
		super();
		this.width = width;
		this.height = height;
		svg = "<svg xmlns=\"http://www.w3.org/2000/svg\" width=\"" + width + "\" height=\"" + height
				+ "\" version=\"1.1\">" + Constants.NEW_LINE;
		init();
	}

	public void init() {
		// translate by the padding factor of drawingAreaWidth - 1
		svg += "<rect width=\"" + width + "\" height=\"" + height + "\" stroke=\"white\" fill=\"white\"/>";
		svg += "<g transform=\"translate(" + (padding * 5) + ", " + (padding) + ")\">" + Constants.NEW_LINE;
	}

	public int getDrawingAreaHeight() {
		return drawingAreaHeight;
	}

	public int getDrawingAreaWidth() {
		return drawingAreaWidth;
	}

	public void xaxis(Map<Double, Double> xCoordinates) {
		Map<String, String> style = new HashMap<>();
		style.put("stroke", "black");
		style.put("stroke-width", "2");
		// x
		drawLine(0, drawingAreaHeight, drawingAreaWidth, drawingAreaHeight, style);
		// draw ticks
		final DecimalFormat df = new DecimalFormat("#0.00");
		xCoordinates.forEach((x, value) -> {
			drawLine(x, drawingAreaHeight, x, drawingAreaHeight + 5, style);
			svg += "<text x=\"" + (x + 10) + "\" y=\"" + (drawingAreaHeight + 10)
					+ "\" dy=\".32em\" text-anchor=\"end\">" + df.format(value) + "</text>" + Constants.NEW_LINE;
		});
	}

	public void yaxis(Map<Double, Double> yCoordinates) {
		Map<String, String> style = new HashMap<>();
		style.put("stroke", "black");
		style.put("stroke-width", "2");
		// x
		drawLine(0, 0, 0, drawingAreaHeight, style);
		// draw ticks
		yCoordinates.forEach((y, value) -> {
			drawLine(-5, y, 0, y, style);
			// add text
			svg += "<text x=\"-9\" y=\"" + y + "\" dy=\".32em\" text-anchor=\"end\">" + value + "</text>"
					+ Constants.NEW_LINE;
		});
	}

	public void drawLine(double x1, double y1, double x2, double y2, Map<String, String> style) {
		Optional<String> css = style.entrySet().stream()
				.map(x -> x.getKey() + ":" + x.getValue() + ";")
				.reduce((x, y) -> x + y);

		svg += "<line x1=\"" + x1 + "\" y1=\"" + y1 + "\" x2=\"" + x2 + "\" y2=\"" + y2 + "\" style=\""
				+ css.orElse(Constants.EMPTY_STRING) + "\" />" + Constants.NEW_LINE;
	}

	public void drawLine(double x1, double y1, double x2, double y2) {
		Map<String, String> style = new HashMap<>();
		style.put("stroke", "lightblue");
		style.put("stroke-width", "2");
		drawLine(x1, y1, x2, y2, style);
	}

	public void drawCircle(double x, double y, double r, String fill) {
		svg += "<circle cx=\"" + x + "\" cy=\"" + y + "\" r=\"" + r
				+ "\" style=\"stroke:steelblue;stroke-width:2\" fill=\"" + fill + "\"/>" + Constants.NEW_LINE;
	}

	@Override
	public String toString() {
		return svg + "</g></svg>";
	}
}
