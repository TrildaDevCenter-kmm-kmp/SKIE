package `tests`.`sealed`.`classes`.`configuration`.`global`.`parent`.`else_name`

import co.touchlab.skie.configuration.SealedInterop

sealed class A

@SealedInterop.Case.Hidden
class A1(val i: Int) : A()
class A2(val k: Int) : A()