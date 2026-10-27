package io.omixer.rpm.parsers;

/**
 * A line of an input file, with where it comes from
 *
 * @param source the name of the file
 * @param number the 1-based line number in that file
 * @param text the content of the line
 */
public record InputLine(String source, int number, String text) {
}
