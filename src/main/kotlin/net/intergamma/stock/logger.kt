package net.intergamma.stock

fun Any.logger() : org.slf4j.Logger = org.slf4j.LoggerFactory.getLogger(this::class.java)