# AwesomeGIC Bank - Banking System Application

## Overview

AwesomeGIC Bank is a simple yet comprehensive banking system that provides core banking functionality through a console-based interface. The application allows users to manage accounts, process transactions, define interest rules, and generate account statements.

## Features

- **Transaction Management**: Deposit and withdraw funds from accounts
- **Interest Rules**: Define and apply interest rates based on date ranges
- **Statement Generation**: View monthly account statements with transaction history and interest calculations
- **User-friendly Interface**: Simple console interface with clear prompts and formatting

## System Architecture

The application follows a clean architecture with separation of concerns:

- **Service Layer**: Contains business logic for accounts, transactions, interest rules, and statements
- **Repository Layer**: Handles data persistence
- **Model Layer**: Defines core domain objects
- **Menu Layer**: Manages user interaction

## Running the Application

To run the application:

1. Compile the project using Maven
2. Run the main application class

## Usage Guide

The application provides a text-based menu system with the following options:

### Main Menu

```
Welcome to AwesomeGIC Bank! What would you like to do?
[T] Input transactions 
[I] Define interest rules
[P] Print statement
[Q] Quit
>
```

### Input Transactions

Format: `<Date> <Account> <Type> <Amount>`

Example: `20230626 AC001 D 100.00`

- Date: YYYYMMdd format
- Account: Any string identifier
- Type: D (deposit) or W (withdrawal)
- Amount: Positive number with up to 2 decimal places

### Define Interest Rules

Format: `<Date> <RuleId> <Rate in %>`

Example: `20230615 RULE03 2.20`

- Date: YYYYMMdd format
- RuleId: Any string identifier
- Rate: Interest rate as a percentage (between 0 and 100)

### Print Statement

Format: `<Account> <YearMonth>`

Example: `AC001 202306`

- Account: Account identifier
- YearMonth: Format YYYYMM

