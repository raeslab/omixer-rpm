# Omixer-RPM
### A tool to generate metabolic modules profiles from metagenomic samples.

Depends on Java8.

usage: java -jar omixer-rpm.jar  [-a <ANNOTATION>] [-c <COVERAGE>] [-d <FILE>] [-e <FORMAT>] [-h] [-i <PATH>] [--ignore-taxonomic-info] [-n] [-o <DIRECTORY>] [-s <SCORE-ESTIMATOR>] [-t <THREADS>] [--Xdistribute]

<pre>
 -a,--annotation <ANNOTATION>             Input file annotation.
                                          Use 1 for orthologs only files or 2 for taxonomic annotation followed by orthologs.
                                          Defaults to 1
 -c,--coverage <COVERAGE>                 The minimum coverage cut-off to accept a module [0.0 to 1.0].
                                          Defaults to -1, where the coverage is learned from the coverage distribution of all modules
 -d,--database <FILE>                     The path to the modules database
 -e,--export-format <FORMAT>              The output file format.
                                          Use 1 for single tab separated files containing module id, abundance and coverage. Use 2 for an abundance and a coverage matrices.
                                          Defaults to 1.
 -h,--help                                Show this help message and exit
 -i,--input <PATH>                        Path to the input matrix or input directory with one file per sample
    --ignore-taxonomic-info               Ignore taxonomic info from input file and infer modules for the whole metagenome instead
 -n,--normalize-by-length                 Divide module score by its length. When combined with a median estimator, missing reactions (score = 0 )
                                          are included when estimating the median. If the estimated score equals zero then it is replaced by
                                          the minimum observed reaction score. If this option is specified, score calculation is based only on
                                          the number of observed reactions
 -o,--output-dir <DIRECTORY>              Path to the output directory
 -s,--score-estimator <SCORE-ESTIMATOR>   The score estimatore.
                                          Accepted values are [median|average].
                                          Defaults to median
 -t,--threads <THREADS>                   Number of threads to use when mapping the modules.
                                          Defaults to 1
    --Xdistribute                         Experimental feature - When an ortholog is shared by N modules then its abundance is divided by N.
</pre>

### License
[Academic Non-commercial Software License Agreement](../master/LICENSE)

##### Developed by Youssef Darzi [(@omixer)](https://github.com/omixer)