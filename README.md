# Software BigNumberMath Library Package (Stub)

This is a JavaCard library package that provides a stub implementation of big number modular arithmetic operations. All operations throw `ISOException` with error code `0x6A80` (wrong parameters).

This is intended as a placeholder implementation when the JCOP Math library is not available, or as a starting point for implementing a pure software-based modular arithmetic library.

## Package AID

`0xA0:0x00:0x00:0x08:0x04:0x00:0x02`

## Package Name

`im.status.keycard.math`

## Version

`1.0`

## Class

`BigNumberMath` - Stub implementation that throws exceptions for all operations

### Methods

- `modAdd(byte[] a, short aOff, short aLen, byte[] b, short bOff, short bLen, byte[] n, short nOff, short nLen)` - Modular addition: a = (a + b) mod n - Throws ISOException
- `modMul(byte[] a, short aOff, short aLen, byte[] b, short bOff, short bLen, byte[] n, short nOff, short nLen)` - Modular multiplication: a = (a * b) mod n - Throws ISOException
- `modSub(byte[] a, short aOff, short aLen, byte[] b, short bOff, short bLen, byte[] n, short nOff, short nLen)` - Modular subtraction: a = (a - b) mod n - Throws ISOException
- `modRed(byte[] a, short aOff, short aLen, byte[] n, short nOff, short nLen)` - Modular reduction: a = a mod n - Throws ISOException

## Building

```bash
./gradlew build
```

## Dependencies

- None (pure JavaCard API)

## Usage

This library package provides a stub implementation. To use actual modular arithmetic, you must implement the methods in this class or use the `jcop-math` package instead.

## Implementation Notes

To implement actual modular arithmetic operations:

1. Replace the stub methods with your implementation
2. Ensure your implementation handles 256-bit numbers correctly
3. Test thoroughly for edge cases and performance

## License

Same as the main keycard-nssa project.
