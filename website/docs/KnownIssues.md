---
sidebar_position: 3
title: Known issues
---

# Known issues

## Classes from Foundation framework are not in scope after adding SKIE

Frameworks produced by Kotlin/Native transitively export Foundation framework by default.
That means that if you import a Kotlin framework in a Swift file, you can use classes from Foundation even without explicitly importing Foundation.

However, frameworks produced by SKIE do not transitively export Foundation framework because that can cause name conflicts between Kotlin classes and Foundation classes.
These name collisions can only happen in frameworks that contain some Swift code - like the ones produced by SKIE.
Which is why, the Kotlin compiler can use the transitive export but SKIE can't.

The solution is to explicitly import Foundation framework in all files that import the Kotlin framework.
This can be done quickly by finding all occurrences of `import YourKotlinModuleName` and replacing them with:

```
import YourKotlinModuleName
import Foundation
```

## Significantly increased compilation time

We have observed that compilation time can increase a lot in project that export too many functions with default arguments.
Because default arguments are implemented by generating multiple overloads, exporting a function with `n` default arguments will generate `2^n` overloads.
Usually this is not a problem because default arguments are not used that often.
However, some coding patterns produce a lot of functions with default arguments without the developers even realizing that.

Some examples:
- data classes
  - Each data class has a `copy` method with default arguments for all parameters of primary constructor.
- constructor classes used for serialization (e.g. classes annotated by `@Serializable` from `kotlinx.serialization`)
  - Kotlin serialization encourages the use of default arguments to deal with optional fields.

:::note Feedback needed

If you are experiencing this problem, please let us know, because that will help us find more problematic situations and implement appropriate countermeasures.

:::

To mitigate this problem, you can for now use [Configuration](/docs/Configuration/Configuration.md) to decrease the number of generated overloads.
Start by looking for problematic classes that are exported to Swift but whose methods are not being called from Swift (like the classes used for serialization).
Right now, you can disable default arguments only by matching names of packages, classes and functions.
However, we are working on a better configuration API that will make it easier to selectively disable default arguments based on types of functions (constructors, copy methods of data classes, etc.).

:::tip

In general, we strongly recommend to limit the number of exported classes because that has negative impacts on compilation time and binary size even if you don't use SKIE.

:::

## Kotlin/Native compiler caching is disabled

SKIE currently does not support Kotlin compiler caching of 3rd party dependencies.
As a result you might notice a slightly longer compilation time for your debug builds of static frameworks.

We are working on a proper solution for this problem for Kotlin 1.8.20 and upwards.
(We are currently not considering to implement support for caching in Kotlin 1.8.0 and 1.8.10 because Kotlin 1.8.20 introduced a new caching mechanism that replaces the old one.)

:::caution

Based on our internal testing we do not expect a significant compilation performance hit - but if you run into problems with compilation time, please let us know.

:::