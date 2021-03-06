package com.dxc.assignment.repository;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.dxc.assignment.model.Authority;
import com.dxc.assignment.model.Project;
import com.dxc.assignment.model.User;

@Component
@Configuration
@EnableScheduling
public class DbSeeder implements CommandLineRunner {
	
	private UserRepository userRepository;
	private FitnesseRepository fitnesseRepository;
	private ProjectRepository projectRepository;
	
	public DbSeeder(UserRepository userRepository, FitnesseRepository fitnesseRepository, ProjectRepository projectRepository) {
		this.userRepository = userRepository;
		this.fitnesseRepository = fitnesseRepository;
		this.projectRepository = projectRepository;
	}
	@Override
	public void run(String... args) throws Exception {
		User user = new User("user","$2a$04$Vbug2lwwJGrvUXTj6z7ff.97IzVBkrJ1XfApfGNl.Z695zqcnPYra", Arrays.asList(
					new Authority("ROLE_USER")
				));
		this.projectRepository.deleteAll();
		for (String project : fitnesseRepository.getProjects()) {
			Project data = new Project(project, fitnesseRepository.getPieChart(project), fitnesseRepository.getAreaChart(project));
			this.projectRepository.save(data);
		}
		
		
		this.userRepository.deleteAll();
		this.userRepository.save(user);
	}
	
	@Scheduled(fixedDelay = 60000*10)
	public void autoSaveData() throws IOException {
		for (String project : fitnesseRepository.getProjects()) {
			Project data = new Project(project, fitnesseRepository.getPieChart(project), fitnesseRepository.getAreaChart(project));
			this.projectRepository.save(data);
		}
	}
}
