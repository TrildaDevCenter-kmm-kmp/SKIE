let a: Kotlin = Kotlin.A1()

switch onEnum(of: a) {
    case .A1(_):
        exit(0)
    case .A2(_):
        exit(1)
}
