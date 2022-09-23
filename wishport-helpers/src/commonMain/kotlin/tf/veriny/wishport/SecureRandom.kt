package tf.veriny.wishport

import kotlin.random.Random

/**
 * A [Random] implementation that generates cryptographically secure random numbers using
 * operating system facilities.
 */
public expect object SecureRandom : Random