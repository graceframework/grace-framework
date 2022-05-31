import grails.spring.DataSource
beans {
	dataSource(DataSource) {
		driverClassName = "org.h2.Driver"
		url = "jdbc:h2:mem:grailsDB"
		username = "sa"
		password = ""
	}
}
