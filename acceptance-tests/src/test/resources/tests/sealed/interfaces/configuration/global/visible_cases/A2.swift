# sealed-visibleCases = false

let a: A = A2(k: 0)

switch exhaustively(a) {
    case .Else:
        exit(1)
    case .A2(let a2):
        exit(a2.k)
}
