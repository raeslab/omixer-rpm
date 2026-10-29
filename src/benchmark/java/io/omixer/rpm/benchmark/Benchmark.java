package io.omixer.rpm.benchmark;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Random;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * Benchmark gate: runs the current jar and a baseline jar on the same generated inputs, on the same machine, and
 * fails when the current jar needs more heap, is slower than the tolerance, or writes different matrices.
 *
 * <pre>
 * Benchmark &lt;current jar&gt; &lt;baseline jar&gt; &lt;baseline url&gt; &lt;work directory&gt; &lt;synthetic inputs&gt; &lt;module database&gt;
 * </pre>
 *
 * The baseline jar is downloaded from the url when it does not exist.
 */
public final class Benchmark {

	/** Runs per jar and case; the fastest one counts */
	private static final int RUNS = 3;
	/** How much slower than the baseline the current jar may be */
	private static final double TOLERANCE = 1.10;
	/** Matrices compared between the two jars */
	private static final List<String> MATRICES = List.of("modules.tsv", "modules-coverage.tsv");

	private record Case(String name, String input, String heap, List<String> arguments) {}

	private record Run(boolean succeeded, double seconds, Path outputDir) {}

	private Benchmark() {
	}

	public static void main(String[] args) throws IOException, InterruptedException {
		if (args.length != 6) {
			throw new IllegalArgumentException(
					"Usage: Benchmark <current jar> <baseline jar> <baseline url> <work directory> <synthetic inputs> <module database>");
		}
		Path current = Path.of(args[0]);
		Path baseline = Path.of(args[1]);
		Path work = Path.of(args[3]);
		Path synthetic = Path.of(args[4]);
		Path database = Path.of(args[5]);
		if (Files.notExists(baseline)) {
			download(URI.create(args[2]), baseline);
		}
		Path inputs = Files.createDirectories(work.resolve("inputs"));
		scale(synthetic.resolve("synth-matrix.tsv"), inputs.resolve("ko-1000.tsv"), 1000, 1);
		scale(synthetic.resolve("synth-taxon-matrix.tsv"), inputs.resolve("taxon-200.tsv"), 200, 2);

		List<Case> cases = List.of(
				new Case("1,000 samples, KO matrix", "ko-1000.tsv", "128m", List.of("-a", "1", "-e", "2")),
				new Case("1,000 samples, --Xdistribute", "ko-1000.tsv", "192m", List.of("-a", "1", "-e", "2", "--Xdistribute")),
				new Case("200 samples per taxon", "taxon-200.tsv", "512m", List.of("-a", "2", "-e", "2")));

		List<String> failures = new ArrayList<>();
		System.out.printf(Locale.ROOT, "%-32s %6s %12s %12s %7s%n", "case", "heap", "baseline s", "current s", "ratio");
		for (Case benchmarkCase : cases) {
			List<String> arguments = new ArrayList<>(List.of("-i", inputs.resolve(benchmarkCase.input()).toString(),
					"-d", database.toString()));
			arguments.addAll(benchmarkCase.arguments());
			Run baselineRun = fastest(baseline, benchmarkCase.heap(), arguments, work.resolve("baseline"));
			Run currentRun = fastest(current, benchmarkCase.heap(), arguments, work.resolve("current"));
			double ratio = currentRun.seconds() / baselineRun.seconds();
			System.out.printf(Locale.ROOT, "%-32s %6s %12.2f %12.2f %7.2f%n", benchmarkCase.name(), benchmarkCase.heap(),
					baselineRun.seconds(), currentRun.seconds(), ratio);
			if (!baselineRun.succeeded()) {
				failures.add(benchmarkCase.name() + ": the baseline jar failed (-Xmx" + benchmarkCase.heap() + ")");
			} else if (!currentRun.succeeded()) {
				failures.add(benchmarkCase.name() + ": the current jar failed (-Xmx" + benchmarkCase.heap() + ")");
			} else {
				if (ratio > TOLERANCE) {
					failures.add(String.format(Locale.ROOT, "%s: %.2f times the baseline time (limit %.2f)",
							benchmarkCase.name(), ratio, TOLERANCE));
				}
				for (String matrix : MATRICES) {
					if (Files.mismatch(baselineRun.outputDir().resolve(matrix), currentRun.outputDir().resolve(matrix)) != -1) {
						failures.add(benchmarkCase.name() + ": " + matrix + " differs from the baseline");
					}
				}
			}
		}
		if (!failures.isEmpty()) {
			throw new IllegalStateException("Benchmark failed:\n  " + String.join("\n  ", failures));
		}
	}

	/** Runs a jar RUNS times; the result is the fastest run, or the first failure */
	private static Run fastest(Path jar, String heap, List<String> arguments, Path outputRoot)
			throws IOException, InterruptedException {
		Run fastest = null;
		for (int i = 0; i < RUNS; i++) {
			Path outputDir = outputRoot.resolve("run-" + i);
			deleteDirectory(outputDir);
			List<String> command = new ArrayList<>(List.of(javaCommand(), "-Xmx" + heap, "-jar", jar.toString()));
			command.addAll(arguments);
			command.addAll(List.of("-o", outputDir.toString()));
			long start = System.nanoTime();
			Process process = new ProcessBuilder(command).redirectErrorStream(true)
					.redirectOutput(ProcessBuilder.Redirect.DISCARD).start();
			// before 1.2.1 the jar exits with 0 on errors too, so a run also needs its matrices
			boolean succeeded = process.waitFor() == 0
					&& MATRICES.stream().allMatch(matrix -> Files.exists(outputDir.resolve(matrix)));
			Run run = new Run(succeeded, (System.nanoTime() - start) / 1e9, outputDir);
			if (!succeeded) {
				return run;
			}
			if (fastest == null || run.seconds() < fastest.seconds()) {
				fastest = run;
			}
		}
		return fastest;
	}

	/** The java launcher running this benchmark, so that both jars run on the same JDK */
	private static String javaCommand() {
		return ProcessHandle.current().info().command().orElse("java");
	}

	/**
	 * Writes a matrix with the given number of samples by repeating the sample columns of a source matrix, each value
	 * multiplied by a random factor between 0.5 and 1.5 (fixed seed, so the output is the same on every run).
	 */
	private static void scale(Path source, Path target, int samples, int keyColumns) throws IOException {
		Random random = new Random(1);
		List<String> lines = Files.readAllLines(source);
		String[] header = lines.get(0).split("\t", -1);
		int sourceSamples = header.length - keyColumns;
		List<String> output = new ArrayList<>();
		output.add(String.join("\t", Arrays.copyOf(header, keyColumns)) + "\t"
				+ IntStream.rangeClosed(1, samples).mapToObj(i -> String.format(Locale.ROOT, "S%04d", i))
						.collect(Collectors.joining("\t")));
		for (String line : lines.subList(1, lines.size())) {
			String[] fields = line.split("\t", -1);
			StringBuilder row = new StringBuilder(String.join("\t", Arrays.copyOf(fields, keyColumns)));
			for (int i = 0; i < samples; i++) {
				double value = Double.parseDouble(fields[keyColumns + i % sourceSamples]) * (0.5 + random.nextDouble());
				row.append('\t').append(String.format(Locale.ROOT, "%.3f", value));
			}
			output.add(row.toString());
		}
		Files.write(target, output);
	}

	private static void deleteDirectory(Path directory) throws IOException {
		if (Files.exists(directory)) {
			try (var paths = Files.walk(directory)) {
				for (Path path : paths.sorted(Comparator.reverseOrder()).toList()) {
					Files.delete(path);
				}
			}
		}
	}

	private static void download(URI url, Path target) throws IOException, InterruptedException {
		Files.createDirectories(target.getParent());
		HttpClient client = HttpClient.newBuilder().followRedirects(HttpClient.Redirect.NORMAL).build();
		HttpResponse<Path> response = client.send(HttpRequest.newBuilder(url).build(),
				HttpResponse.BodyHandlers.ofFile(target));
		if (response.statusCode() != 200) {
			Files.deleteIfExists(target);
			throw new IOException("Could not download the baseline jar from " + url + " (HTTP " + response.statusCode()
					+ "); set -Pbenchmark.baseline=<jar>");
		}
	}
}
