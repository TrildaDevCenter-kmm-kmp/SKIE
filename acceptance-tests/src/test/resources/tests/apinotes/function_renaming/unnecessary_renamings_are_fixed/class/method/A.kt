package `tests`.`apinotes`.`function_renaming`.`unnecessary_renamings_are_fixed`.`class`.`method`

class A {

    fun foo(i: Int) = i

    fun foo(i: String) = i.toInt()
}