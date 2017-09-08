#!/usr/bin/env Rscript
cairo_pdf("energy.pdf")
data <- read.csv("../results/all/nonTransaction/energy.csv") # please update the root path to the Java project

par(mfrow=c(2,1)) # width:height = 3:1

mar.default <- c(5,4,4,2) + 0.1
par(mar = mar.default + c(0, 0.2, 0, 0))	#add more y-label space in case cut-off

boxplot(data, ylim=c(0, 270), xlab="", ylab="", las = 1, cex.axis = 1.3, boxlwd = 0.5, whisklwd = 0.5, staplelwd = 0.5, medlwd = 0.5, outline = FALSE)

title(ylab = "Energy Consumption (mJ)", xlab = "API/query ID", cex.lab = 1.3, line = 3)

dev.off()

