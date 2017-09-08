#!/usr/bin/env Rscript
data <- read.csv("../results/all/nonTransaction/energy-time.csv")	# please update the root path to the Java project

cor(data, use = "all.obs")
cor.test(data$Time, data$Energy, method="pearson")	#pearson, spearman or kendall

