package `tests`.`apinotes`.`naming`.`unnecessary_renamings_are_fixed`.`class`.`extension`

import tests.apinotes.naming.necessary_renamings.property_and_method_collision.`class`.extension.A

class A

fun A.foo(i: Int) = i

fun A.foo(i: String) = i.toInt()
