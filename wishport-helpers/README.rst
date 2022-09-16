Wishport Functional
===================

This contains common code used by all Wishport modules for functional, monadic error handling.
More information is in the Wishport docs, but this module is fully independent of other modules
and can be used in your own native projects with or without Wishport Core.

Either
------

The ``Either`` monad is a simple result monad. Unlike Kotlin's ``Result``, there's no restrictions
on the error type. You can chain calls monadically using ``andThen`` (the equiv. to the terribly
named ``flatMap``).

Cancellable
-----------

Cancellable adds an extra dimension to an ``Either``.