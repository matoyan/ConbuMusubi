package com.matoyan.conbumusubi;

import java.io.File;

public class PostDataSingleton {
	private static PostDataSingleton instance;
	private PostDataSingleton(){
		instance = null;
	}
	public static PostDataSingleton getInstance(){
		if(instance == null){
			instance = new PostDataSingleton();
		}
		return instance;
	}

	public File mTmpImg;
	
}
