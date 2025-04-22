package org.awesomegic;

import org.awesomegic.config.BankingConfiguration;
import org.awesomegic.menu.BankingMenu;

import java.util.Scanner;

public class SimpleBankingApp
{
    public static void main( String[] args )
    {
        BankingConfiguration config = BankingConfiguration.getInstance();

        new BankingMenu(new Scanner(System.in),
                config.getAccountService(),
                config.getTransactionService(),
                config.getInterestRuleService(),
                config.getStatementService()).start();
    }
}
