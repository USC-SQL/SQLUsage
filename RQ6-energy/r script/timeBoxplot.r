#!/usr/bin/env Rscript
cairo_pdf("time.pdf")
data <- read.csv("../results/all/nonTransaction/time.csv") # please update the root path to the Java project
library("MASS")

mar.default <- c(5,4,4,2) + 0.1
par(mar = mar.default + c(0, 0.2, 0, 0))	#add more y-label space in case cut-off

boxplot(data, ylim=c(0, 55), xlab="API", ylab="Time (ms)", las = 1, cex.axis = 0.5, boxlwd = 0.5, whisklwd = 0.5, staplelwd = 0.5, medlwd = 0.5, outline = FALSE)

dev.off()

