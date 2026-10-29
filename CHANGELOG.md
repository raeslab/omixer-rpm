# Changelog

## Unreleased (2.0.0)

### Requirements
- **Java 21 or newer** (1.2: Java 17).

### Fixed
- **B26** `-e 1` with `--ignore-taxonomic-info` on a taxon input (`-a 2`) writes the `Module Value Coverage` layout.
  Before, the files had a `Taxon` column holding the text `null`. Values are unchanged.
- **B27** Module database: a step alternative without a KO (e.g. the line `,`) stops the run with the file and line.
  Before, it became an empty alternative (`NaN` abundance with `--complex-rule all`).

## 1.2.1 (2026-10-28)

### Fixed
- **B25** The jar exits with a non-zero status on errors: 2 for a command line error, 1 for an input or run error.
  Before, it exited with 0, so scripts, pipelines and the `omixerRpm` R package could not detect a failed run.
  Results are unchanged.

## 1.2 (2026-10-28)

Compared with 1.1. Unless an item under "Changed results" applies, 1.2 gives the same numbers as 1.1.

### Requirements
- **Java 17 or newer** (1.1 ran on Java 8).

### Added
- `--complex-rule any|all`: how a complex step (`K1,K2` in the module database) counts when only some subunits are
  observed. `any` (default, same as 1.1): observed when at least one subunit is; `all`: only when every subunit is,
  otherwise the step gets no coverage and no abundance.

### Changed results
- **B01** Per-taxon `--Xdistribute` (`-a 2`) reports every taxon. Before, only the last taxon of each sample was
  kept, under every taxon's name.
- **B02** `--Xdistribute` splits a KO by the number of **distinct modules** using it, at every occurrence. Before,
  a KO repeated inside one module was counted once per occurrence (so a module could be split against itself), and
  only one occurrence received the reduced value.
- **B03** `-s min` returns the smallest abundance of any present KO. Before, within the first observed step the KOs
  overwrote each other, so for a complex the last subunit listed in the module database was used instead of the
  smallest one: results depended on the subunit order. Only modules whose first observed step is a complex are
  affected, and their values can only go down.
- **B07** Blank lines inside a module in the module database are no longer read as empty steps (which lowered
  coverage silently).
- **B16** Modules with no observed step get abundance 0 instead of `NaN` (`-s median` / `-s average` with `-c 0`).
- **B05** Directory input: a sample is named after its file without the **last** extension only (`run.v2.tsv` →
  `run.v2`; before, `run`).
- **B19** Matrix output (`-e 2`) lists samples in input order (matrix header order; file-name order for a
  directory) and modules in module-database order. Before, both followed hash order.

### Fixed — errors instead of silently wrong results
- **B05** Directory input: two files giving the same sample name stop the run with an error naming both. Before,
  one silently replaced the other.
- **B06** Matrix input: a sample name that appears twice in the header stops the run with an error. Before, the
  two columns were silently added together.
- **B13** Directory input: hidden files (e.g. `.DS_Store`) and sub-directories are skipped. A visible file that
  isn't a sample stops the run with an error naming the file. Before: a crash or a misleading message, and a hidden
  file was read as a sample with an empty name.
- **B17** `-c` values outside 0–1 (other than -1 for automatic), including `NaN`, are rejected.

### Fixed — crashes and error messages
- **B10** Per-taxon `--Xdistribute` no longer crashes with a misleading "wrong --annotation" error.
- **B11** A blank line after a `///` (at the end of the module database, or between modules) no longer crashes the run.
- **B12** An input where no module is observed in any sample no longer crashes the automatic cutoff: it warns,
  reports no module and logs the cutoff as "none".
- **B21–B23** Module database errors are reported with the file name and line number: a header without a name, a
  `///` where a header is expected, and a module without steps. Before, these crashed (with a misleading
  "wrong --annotation" message, or a raw stack trace).
- **B24** Matrix input: a value that is not a number (including a taxon matrix read with `-a 1`) stops the run with
  the file, line and expected format, instead of a raw `NumberFormatException`. Function ids that read as numbers
  (e.g. a plain matrix read with `-a 2`) give a warning to check `-a`.
- **B15** The generic "make sure you are using the correct annotation value" message is gone; errors say what is
  wrong and where.
- **B20** The error for an invalid `-s` value lists all accepted values (`median|average|sum|min`); before, it only
  mentioned median and average.

### Build
- Gradle 9.7, Spock 2.5 / Groovy 5 tests, Log4j 2, current commons libraries.
- Packages renamed `org.omixer.*` → `io.omixer.*`; depends on `io.omixer:omixer-utils:1.1.0`.
