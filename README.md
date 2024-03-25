
# income-tax-interest

This is where we make API calls from users for creating, viewing and making changes to the interest section of their income tax return.

## Running the service locally

You will need to have the following:
- Installed/configured [service manager V2](https://github.com/hmrc/sm2).

The service manager profile for this service is:

    sm2 --start INCOME_TAX_INTEREST
Run the following command to start the remaining services locally:

    sudo mongod (If not already running)
    sm2 --start INCOME_TAX_SUBMISSION_ALL -r

This service runs on port: `localhost:9309`

### Running Tests
- Run Unit Tests:  `sbt test`
- Run Integration Tests: `sbt it/test`
- Run Unit and Integration Tests: `sbt test it/test`
- Run Unit and Integration Tests with coverage report: `sbt runAllChecks`<br/>
  which runs `clean compile scalastyle coverage test it/test coverageReport`

### Interest endpoints:

**GET     /income-tax/income-sources/nino/:nino?incomeSourceType=interest-from-uk-banks** (Retrieves a list of data relating to the interest income source type)

**GET     /income-tax/nino/:nino/income-source/savings/annual/:taxYear?incomeSourceId=:incomeSourceId** (Retrieves details for the interest income source over the accounting period which matches the tax year provided)

**POST    /income-tax/income-sources/nino/:nino** (Creates a skeleton interest income source record for the supplied nino)

**POST    /income-tax/nino/:nino/income-source/savings/annual/:taxYear** (Provides the ability for a user to submit periodic annual income for interest)

### Downstream services

All interest data is retrieved/updated via the downstream system.

- DES (Data Exchange Service)

### Interest income Source

<details>
<summary>Click here to see an example of a user interest data (JSON)</summary>

```json
[
  {
      "incomeSourceId": "000000000000001",
      "incomeSourceName": "Bank Account 1",
      "identifier": "AA111111A",
      "incomeSourceType": "interest-from-uk-banks"
  },
  {
      "incomeSourceId": "000000000000002",
      "incomeSourceName": "Bank Account 2",
      "identifier": "AA111111A",
      "incomeSourceType": "interest-from-uk-banks"
  },
  {
      "incomeSourceId": "000000000000003",
      "incomeSourceName": "Bank Account 3",
      "identifier": "AA111111A",
      "incomeSourceType": "interest-from-uk-banks"
  }
]
```

</details>

## Ninos with stubbed data for interest

| Nino      | Interest data                           |
|-----------|-----------------------------------------|
| AA123459A | User with multiple interest accounts    |
| AA000002A | User with interest accounts end of year |
| AA000003A | User with multiple interest accounts    |

### License

This code is open source software licensed under the [Apache 2.0 License]("http://www.apache.org/licenses/LICENSE-2.0.html").
