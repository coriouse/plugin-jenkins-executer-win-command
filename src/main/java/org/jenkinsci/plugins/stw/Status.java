package org.jenkinsci.plugins.stw;

public enum Status {
	OK, //Без ошибок завершение
	ERROR, // Есть ошибки завершение
	STEP; // В просессе выполнения	
}