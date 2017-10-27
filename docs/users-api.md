
# Users API

Here is an example POTS on /users endpoint.

#### Headers
```
POST /v1/users HTTP/1.1
Host: 193.67.163.82
Content-Type: application/json
Origin: https://www.britishgas.co.uk
X-Client-ID: 4fc507b2-738f-49bc-8ea8-437cdc37acd0
X-Backend-Users-Key: ABC
```

#### Body

```json
{
    "data": {
        "id": "00300535746105",
        "type": "users",
        "attributes": {
            "brands": [
                "BG"
            ],
            "channel": "paper",
            "title": "Mr",
            "firstName": "Jon",
            "surname": "Patrick",
            "email": "jon.jackson@centrica.com",
            "status": "active"
        }
    }
}
```