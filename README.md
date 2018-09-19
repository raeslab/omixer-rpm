# Omixer-RPM
A **R**eference **P**athways **M**apper for turning metagenomic functional profiles into pathway/module profiles.

#### Usage
java -jar [omixer-rpm.jar](../../releases/latest)  [-a <ANNOTATION>] [-c <COVERAGE>] [-d <FILE>] [-e <FORMAT>] [-h] [-i <PATH>] [--ignore-taxonomic-info] [-n] [-o <DIRECTORY>] [-s <SCORE-ESTIMATOR>] [-t <THREADS>] [--Xdistribute]

<pre>
 -a,--annotation <ANNOTATION>             Input file annotation.
                              Use 1 for orthologs only files or 2 for taxonomic annotation followed by orthologs.
                              Defaults to 1
 -c,--coverage <COVERAGE>               The minimum coverage cut-off to accept a module [0.0 to 1.0].
                              Defaults to -1, where the coverage is learned from the coverage distribution of all modules
 -d,--database <FILE>               The path to the modules database
 -e,--export-format <FORMAT>          The output file format.
                              Use 1 for single tab separated files containing module id, abundance and coverage. 
                              Use 2 for an abundance and a coverage matrices.
                              Defaults to 1.
 -h,--help                    Show this help message and exit
 -i,--input <PATH>                  Path to the input matrix or input directory with one file per sample
    --ignore-taxonomic-info   Ignore taxonomic info from input file and infer modules for the whole metagenome instead
 -n,--normalize-by-length     Divide module score by its length. When combined with a median estimator, missing reactions (score = 0 )
                              are included when estimating the median. If the estimated score equals zero then it is replaced by
                              the minimum observed reaction score. If this option is specified, score calculation is based only on
                              the number of observed reactions
 -o,--output-dir <DIRECTORY>             Path to the output directory
 -s,--score-estimator <SCORE-ESTIMATOR>        The score estimatore.
                              Accepted values are [median|average].
                              Defaults to median
 -t,--threads <THREADS>                Number of threads to use when mapping the modules.
                              Defaults to 1
    --Xdistribute             Experimental feature - When an ortholog is shared by N modules then its abundance is divided by N.
</pre>

#### Database file format.
The reference pathways database is a flat file where pathway/module reactions are listed following their order in the pathway.
Tab-separated reactions indicate alternative reactions (OR operation), while return- and comma-separated reactions 
are all required for process completeness (AND operation). Below is a snippet from the human gut metabolic modules (GMMs)
database, described in *[Vieira-Silva et al. 2016](https://www.nature.com/articles/nmicrobiol201688)*.
<pre>
MF0001	arabinoxylan degradation
K01209	K15921	K01181	K01198	K15531	K18205
///
MF0003	pectin degradation I
K01051
K01184,K01213	K18650
///
MF0103	mucin degradation
K01186
K05970
K01132	K01135	K01137	K01205
K01207	K12373	K14459
K01205	K01207	K12373	K01227	K13714
K01206
///
</pre>
You can find the complete database [here](https://github.com/raeslab/GMMs/blob/master/GMMs.v1.07.txt).
KEGG Module pathways or custom databases formatted accordingly would also work.

#### Description
As defined in the database snippet above, a metabolic module is a set of alternative ortholog combinations 
that represent a cellular process. Given a metagenome, Omixer-RPM will select the modules that pass a 
user defined coverage (# observed steps / # defined steps) cutoff, then derives their abundance 
by selecting, for each module, the combination of orthologs that maximize its abundance.

For example, given the following module definition and profile
<pre>
I: Glucose -> R1 -> R2 -> R3 -> R4 -> R5 -> O: glyceraldehyde 3-phosphate
R1: K00844 K00845
R2: K01810 K06859 K13810 K15916
R3: K00850 K16370
R4: K01622 K01623 K01624 K11645 K16305 K16306
R5: K01803
</pre>
<pre>
K00844	10
K00845	3
K06859	7
K13810	8
K01804	12
K01622	2
K16370	5
K00703	12
K00863	15
</pre>

The two possible combinations are:
<pre>
A) K00844 (10) -> K13810 (8) -> K01622 (2) -> K16370 (5) -> NA
B) K00845 (3)  -> K06859 (7) -> K01622 (2) -> K16370 (5) -> NA
</pre>
The coverage in this case is 4/5 but the average in A (25/5) is greater than in B (17/5), therefore the selected path is A and the abundance of the module is 5.


#### Citing Omixer-RPM
Omixer-RPM was developed as part of [GOmixer](http://www.raeslab.org/gomixer/). If you use Omixer-RPM in your work please cite: <br />

*Youssef Darzi, Gwen Falony, Sara Silva, Jeroen Raes. [Towards biome-specific analysis of meta-omics data, The ISME journal, 2015](https://www.nature.com/articles/ismej2015188)* 

#### License
[Academic Non-commercial Software License Agreement](../master/LICENSE)

###### Developed by Youssef Darzi [(@omixer)](https://github.com/omixer)
