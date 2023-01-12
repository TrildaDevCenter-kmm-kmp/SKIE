let allValues = [AKt.a1(), AKt.a2()]

for (index, value) in allValues.enumerated() {
    assert(value.immutableProperty == 0, "immutableProperty should be 0")
    assert(value.mutableProperty == 0, "mutableProperty should be 0")
    value.mutableProperty = Int32(index)
    assert(value.mutableProperty == index, "mutableProperty should be \(index)")
    assert(value.abstractImmutableProperty == index + 1, "abstractImmutableProperty should be \(index + 1)")
    assert(value.abstractMutableProperty == index, "abstractMutableProperty should be \(index)")
    assert(value.overridableImmutableProperty == index, "overridableImmutableProperty should be \(index)")
    assert(value.overridableMutableProperty == index, "overridableMutableProperty should be \(index)")
    value.abstractMutableProperty = Int32(index + 2)
    value.overridableMutableProperty = Int32(index + 3)
    if (index == 0) {
        assert(value.abstractMutableProperty == index + 2, "abstractMutableProperty should be \(index + 2), is \(value.abstractMutableProperty)")
        assert(value.overridableMutableProperty == index + 3, "overridableMutableProperty should be \(index + 3)")
    } else {
        assert(value.abstractMutableProperty == 2 * (index + 2), "abstractMutableProperty should be \(2 * (index + 2)), is \(value.abstractMutableProperty)")
        assert(value.overridableMutableProperty == 2 * (index + 3), "overridableMutableProperty should be \(2 * (index + 3))")
    }

    assert(value.extensionProperty == 0, "extensionProperty should be 0")
    assert(value.extensionMutableProperty == 0, "extensionMutableProperty should be 0")
    value.extensionMutableProperty = Int32(index)
    assert(value.extensionPropertyWithSelf == value, "value should be \(value)")
    assert(value.extensionMutablePropertyWithSelf == value, "value should be \(value)")
    value.extensionMutablePropertyWithSelf = value

    switch value {
    case .a1:
        assert(index == 0)
    case .a2:
        assert(index == 1)
    }
}

exit(0)
