let result = await Task.detached {
    try! await (A1() as A).foo(i: 0, k: 1)
}.value

exit(result.int32Value)