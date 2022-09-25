Wishport
========

.. image:: https://archives.bulbagarden.net/media/upload/a/a9/036Clefable.png
    :alt: Clefable
    :target: https://www.smogon.com/forums/threads/teleport-clefable-ou.3661414/

**Wishport** is an asynchronous application library for Kotlin/Native, heavily inspired by `Trio`_
for Python. It uses ``io_uring`` for high-performance asynchronous I/O on Linux, and IOCP for
high-performance asynchronous I/O on Windows (support pending).

Wishport is a single-threaded, co-operative multitasking, and functional asynchronous library.
It is incompatible with ``kotlinx-coroutines`` or libraries written for it.

Installation
------------

Wishport can be added to your Kotlin MPP Gradle project like so:

.. code-block:: kotlin

    repositories {
        mavenCentral()
        mavenLocal()
        maven(url = "https://maven.veriny.tf")
    }

    kotlin {
        sourceSets.getByName("commonMain").apply {
            dependencies {
                api("tf.veriny.wishport:wishport-core:0.7.0")
            }
        }
   }

(Note: Wishport is not yet published.)

Quickstart
----------

See the docs at https://wishport.readthedocs.io/.

.. _Trio: https://github.com/python-trio/trio