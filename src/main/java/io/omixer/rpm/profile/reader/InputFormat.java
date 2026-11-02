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
	 * A HUMAnN table (-a humann), e.g. gene families regrouped to KOs: function or function|taxon, then one abundance
	 * per sample. A function may carry its name (humann_rename_table: "K00001: alcohol dehydrogenase"); an
	 * unstratified row (the community total) has no taxon. Which rows are kept is up to {@link HumannReader}
	 */
	HUMANN(1, "function[|taxon]<TAB>one abundance per sample") {
		@Override
		public String taxon(String[] fields) {
			int stratum = fields[0].indexOf(STRATUM);
			return stratum < 0 ? null : fields[0].substring(stratum + 1);
		}

		@Override
		public String function(String[] fields) {
			int stratum = fields[0].indexOf(STRATUM);
			String function = stratum < 0 ? fields[0] : fields[0].substring(0, stratum);
			int name = function.indexOf(NAME);
			return name < 0 ? function : function.substring(0, name);
		}
	};

	/** Separates a HUMAnN function from its stratum (taxon) */
	private static final char STRATUM = '|';
	/** Separates a HUMAnN function from its name */
	private static final String NAME = ": ";

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
