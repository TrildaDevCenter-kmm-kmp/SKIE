# Skip
# SwiftCompilationError(error: cannot convert value of type 'A<T>' to expected argument type 'A<KotlinInt>')

let result: String = try! await A<KotlinInt>().foo(i: 1)