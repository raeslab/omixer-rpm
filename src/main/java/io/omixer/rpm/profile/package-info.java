/**
 * The input as profiles: {@link io.omixer.rpm.profile.ProfilesBuilder} receives every value from a reader (as a
 * {@link io.omixer.rpm.profile.ProfileSink}) and builds the immutable {@link io.omixer.rpm.profile.Profiles}: one per
 * sample, or per taxon of a sample, each holding only the non-zero values of database orthologs, by ortholog index.
 */
package io.omixer.rpm.profile;
