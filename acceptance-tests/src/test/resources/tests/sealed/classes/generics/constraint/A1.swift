let a: A<IntWrapper> = A1<IntWrapper>(wrapper: IntWrapper(value: 0))

switch exhaustively(a) {
    case .A1(let a1):
        exit(a1.wrapper.value.int32Value)
}
