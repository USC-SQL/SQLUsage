All commands were run in Ubuntu 14.04 with Java 1.8 in our evaluation.

# RQ 1
- The statistics of local database usage for RQ1 are included in RQ1-database-stat
- The information (names and versions) of all 12,506 distinct apps is included in the sheet appVersionInfo [statistics.xlsx]
- The list of 1,000 is included in the file [RQ1-database-stat/1000apps.txt].

# RQ 2, 3, 4, 5, 7, 8 (folder RQ2,3,4,5,7,8-static-analysis)
- By runing the shell script "run.sh" in the RQ2,3,4,5,7,8-static-analysis folder, we can obtain the statistics presented in these RQs. The files [rq2.txt], [rq3-5.txt] and [rq7.txt], [rq8.txt] are the output of our evaluation. The program LogAnalyzer.jar used in the script analyzes the various analysis results and summarizes them. The source code is in "analyzer/code/SQLUsage/src/usc/sql/log/LogSummary.java"
- To build the source code, we prefer to import to eclipse.
- The static analysis results from the raw query string analysis [string.txt] and transaction analysis [apiInLoopTransaction.txt], etc are stored in the RQ2,3,4,5,7,8-static-analysis/results folder. 
    (Optional) To generate these results go to the analyzer folder, and run the script "runAuto.sh". Note that we only provide one example app in the "analyzer/Android/App1" folder. To reproduce the results, please download the 1,000 apps provided in [1000apps.txt], follow the same folder structure as App1 and generate the classlist.txt for each app.

# RQ 6
- Please first download the energy raw data from https://goo.gl/EhpSVU. Unzip the download file and place it under [RQ6-energy].
- The energy raw data and SQLite runtime usage log are stored in the folder [RQ6-energy/raw-data/energy-raw] and [RQ6-energy/raw-data/log-stat]. Note that among all 1,000 apps, there are in total 282 apps [RQ6-energy/results/succ-app-list-all.txt] (out of 583 [RQ6-energy/results/sqliteAppList.txt] with SQLite embedded) whose SQLite database usage was recorded.
- By running the Java program getDbEnergy.jar, we can obtain the energy consumption of SQLite related APIs. The example command is: java -jar getDbEnergy.jar raw-data/energy-raw raw-data/log-stat succ-app-list-all.txt. Note that this step is time consuming (can be over several hours). By updating the app list in the file [RQ6-energy/results/succ-app-list-all.txt], you can reproduce a subset of the results. Please go ahead to the next step if you want to get detailed energy statistics in the following steps. Note in the root directory: (1) the output file overlappingAllStats.txt includes statistics of non-concurrent executed and all APIs. (2) the folder appDbEnergyNonConcurrTransaction/ has the energy consumption of non-concurrent APIs of each of 282 apps, and the energy information of all 282 apps is also output to the file allEnergyNonConcurrTransaction.txt. (3) in the same way, the energy consumption of APIs corresponding to each of 282 apps and all 282 apps is stored in appDbEnergyConcurrTransaction/ and allEnergyConcurrTransaction.txt, respectively. The folder [RQ6-energy/results] is the output of our evaluation.
- By running the Java program getNonTransactionEnergyStats.jar, we can obtain the detailed energy [RQ6-energy/results/all/nonTransaction] of non-transaction APIs, including operations [RQ6-energy/results/all/nonTransaction/operations], APIs [RQ6-energy/results/all/nonTransaction/resultSet]. The energy statistics corresponding to all APIs are collected together in a single file [RQ6-energy/results/all/nonTransaction/allEnergyStat.txt]. The example command is: java -jar getNonTransactionEnergyStats.jar succ-app-list-all.txt results/appDbEnergyNonConcurrTransaction, where appDbEnergyNonConcurrTransaction is the output folder in the previous step.
- All energy statistics are included in the file [statistics.xlsx]: API energy in sheet apiEnergy (ascending order by the mean energy), operation energy in sheet operEnergy, energy ranking with both API and operation energy in sheet apiAndOperEnergyRanking.
- The post processing program is used to generate the files with specific format so as to draw the boxplot or correlation test using R script. The example command is: java -jar postProcessAllEnergy.jar results/all/nonTransaction/allDetailedAPIEnergy.csv results/all/nonTransaction/allDetailedOperEnergy.csv
- This file is used by the R script energyBoxplot.r to generate the boxplot figure as shown in the paper. (command: Rscript energyBoxplot.r) (install R package using sudo apt-get install r-base)
- Run the R script correlation.r to conduct the Pearson correlation test. (command: Rscript correlation.r)

