/**
 * The built-in inference method, scoring: {@link io.omixer.rpm.inference.scoring.ScoringInference} applies the
 * {@link io.omixer.rpm.inference.scoring.SharedKoResolution} (--Xdistribute), scores every module of every profile with
 * a {@link io.omixer.rpm.inference.scoring.ModuleScorer} and resolves the cutoff. The options are an
 * {@link io.omixer.rpm.inference.scoring.Estimator} (-s), a {@link io.omixer.rpm.inference.scoring.ComplexRule}
 * (--complex-rule) and -n ({@link io.omixer.rpm.inference.scoring.ScoringOptions}).
 */
package io.omixer.rpm.inference.scoring;
