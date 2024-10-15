package com.server.server;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;


@SpringBootApplication
public class ServerApplication implements CommandLineRunner{
	// @Autowired
    // private OSMToRoadConverter osmToRoadConverter;
	public static void main(String[] args) {
		
		SpringApplication.run(ServerApplication.class, args);
	}
	@Override
    public void run(String... args) throws Exception {
        // 调用 OSMToRoadConverter 来解析并插入数据
       // osmToRoadConverter.parseOSMData();
       // System.out.println("OSM 数据已解析并插入到数据库中");
    }
}
