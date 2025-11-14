package com.vjaykrsna.nanoai.core.common.annotations

/** Marks APIs that continuously emit updates and must be collected. */
@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
annotation class ReactiveStream(val description: String = "")

/** Marks single-shot suspend APIs that return a [NanoAIResult]. */
@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
annotation class OneShot(val description: String = "")
