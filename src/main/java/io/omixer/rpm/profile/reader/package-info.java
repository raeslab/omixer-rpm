/**
 * Input readers (-a, -i): {@link io.omixer.rpm.profile.reader.ProfileReader#forInput} picks a
 * {@link io.omixer.rpm.profile.reader.MatrixReader} (one matrix), a {@link io.omixer.rpm.profile.reader.DirectoryReader}
 * (one headerless file per sample) or a {@link io.omixer.rpm.profile.reader.HumannReader} (one HUMAnN table, whose rows
 * pass a {@link io.omixer.rpm.profile.reader.HumannRowSelection}). {@link io.omixer.rpm.profile.reader.InputFormat}
 * says what the key columns of a row hold. Readers only parse: they pass every value on, and the sink decides.
 */
package io.omixer.rpm.profile.reader;
