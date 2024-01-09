# App

Program structure
```
admin controlled 
  Creates Users
  Creates Iot Certs
Users
  Iot pub/sub
  create Rigs/things
Rigs
  Iot pub/sub
```

## Code structure
- Gateways - low level wrappers of AWS apis
- Controllers - mid level API that use multiple gateways to do the action (UserControl plane)
- Entities - high level API that capture application use cases
- Datatypes - Immutable datatypes to support the app

Will build this out under app V2:
```
Entities
  Rig
    decode
  User
```

