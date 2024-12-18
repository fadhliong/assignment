# Crypto Trading System

A Spring Boot application that provides a REST API for cryptocurrency trading, wallet management, and real-time price tracking.

## Features

- **Real-time Price Tracking**
  - Monitor live BTC/USDT and ETH/USDT prices at 10 minute intervals
  - Track best bid and ask prices
  - Indexed price data for optimal query performance

- **Trade Execution**
  - Execute buy/sell trades for supported trading pairs
  - Idempotent trade operations
  - Trade validation and error handling
  - Real-time price execution

- **Wallet Management**
  - View wallet balances for USDT, BTC, and ETH
  - Track trade history
  - Paginated trade history with customizable time periods
  - Secure wallet access with user authentication

## API Endpoints

### Price API
```
GET /api/pricing/latest
```
Returns the latest bid/ask prices for BTC/USDT and ETH/USDT pairs.

### Trade API
```
POST /api/trades
```
Execute a new trade with the following required parameters:
- `idempotencyKey`: Unique identifier for trade deduplication
- `userId`: User identifier
- `walletId`: Wallet identifier
- `tradingPair`: Trading pair (e.g., BTC/USDT, ETH/USDT)
- `tradeType`: Buy or Sell
- `amount`: Trade amount
- `price`: Requested execution price

### Wallet API
```
GET /api/wallet/{walletId}/trades
```
Retrieve trade history for a specific wallet with pagination support:
- `page`: Page number (default: 0)
- `size`: Page size (default: 20)
- `timePeriodMonths`: Time period in months (default: 1)

```
GET /api/wallet/balance
```
Get current wallet balances for all supported currencies.

## Data Models

### Price
- Tracks best bid/ask prices for supported trading pairs
- Includes timestamp for price updates
- Optimized with database indexes for quick lookups

### Trade
- Records all trade execution details
- Includes requested and actual execution prices
- Tracks trade status and execution time
- Calculates total market value

### Wallet
- Maintains current balances for USDT, BTC, and ETH
- Tracks last update timestamp
- Links to associated trades

## Security

- User authentication required for all wallet operations
- User ID validation via `X-User-ID` header
- Input validation for all trade parameters
- Idempotency support to prevent duplicate trades

## Technical Details

Built with:
- Spring Boot
- JPA/Hibernate
- Jakarta Validation
- Lombok
- Spring Web
- SLF4J for logging

## Error Handling

The system includes comprehensive validation:
- Trade amount and price validation
- Wallet balance verification
- Trading pair validation
- User authentication checks
- Idempotency key format validation
