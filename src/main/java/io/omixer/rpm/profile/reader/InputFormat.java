package io.omixer.rpm.profile.reader;

/**
 * The layout of a matrix row (-a): the key columns before the abundances, and what they hold.
 */
public enum InputFormat {

	/** KO, then one abundance per sample (-a 1) */
	KO(1, "KO<TAB>one abundance per sample") {
		@Override
		public String taxon(String[] fields) {
			return null;
		}

		@Override
		public String function(String[] fields) {
			return fields[0];
		}
	},

	/** Taxon and KO, then one abundance per sample (-a 2) */
	TAXON_KO(2, "taxon<TAB>KO<TAB>one abundance per sample") {
		@Override
		public String taxon(String[] fields) {
			return fields[0];
		}

		@Override
		public String function(String[] fields) {
			return fields[1];
		}
	},

	/**
	 * HUMAnN2 stratified gene families: KO or KO|taxon, then one abundance per sample; an unstratified row has taxon
	 * NA. Not offered on the command line until the HUMAnN 3/4 rework (BUGS.md B08, B09, B14)
	 */
	HUMANN(1, "KO[|taxon]<TAB>one abundance per sample") {
		@Override
		public String taxon(String[] fields) {
			String[] koTaxon = fields[0].split("\\|");
			return koTaxon.length == 2 ? koTaxon[1] : "NA";
		}

		@Override
		public String function(String[] fields) {
			return fields[0].split("\\|")[0];
		}
	};

	private final int keyColumns;
	private final String matrixRow;

	InputFormat(int keyColumns, String matrixRow) {
		this.keyColumns = keyColumns;
		this.matrixRow = matrixRow;
	}

	/** @return the number of columns before the abundances */
	public int keyColumns() {
		return keyColumns;
	}

	/** @return the expected layout of a matrix row, for error messages */
	public String matrixRow() {
		return matrixRow;
	}

	/** @return the taxon of a row, or null when the format has none; the row has more than {@link #keyColumns()} fields */
	public abstract String taxon(String[] fields);

	/** @return the function id of a row; the row has more than {@link #keyColumns()} fields */
	public abstract String function(String[] fields);
}
