# Software BigNumberMath Library Package (Stub)

This is a JavaCard library package that provides a stub implementation of big number modular arithmetic operations.

## Package AID

`0xA0:0x00:0x00:0x08:0x04:0x00:0x02`

## Package Name

`im.status.keycard.math`

## Version

`1.0`

## Class

`BigNumberMath` - Stub implementation that throws exceptions for all operations

### Methods

- `modAdd(byte[] a, short aOff, short aLen, byte[] b, short bOff, short bLen, byte[] n, short nOff, short nLen)` - Modular addition: a = (a + b) mod n - Implemented
- `modMul(byte[] a, short aOff, short aLen, byte[] b, short bOff, short bLen, byte[] n, short nOff, short nLen)` - Modular multiplication: a = (a * b) mod n - Throws ISOException
- `modSub(byte[] a, short aOff, short aLen, byte[] b, short bOff, short bLen, byte[] n, short nOff, short nLen)` - Modular subtraction: a = (a - b) mod n - Implemented
- `modRed(byte[] a, short aOff, short aLen, byte[] n, short nOff, short nLen)` - Modular reduction: a = a mod n - Throws ISOException

## Building

```bash
./gradlew build
```

## Dependencies

- None (pure JavaCard API)

## Usage

This library package provides a software implementation for modular arithmetic. Just load it before Keycard's main package
