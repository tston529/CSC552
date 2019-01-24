/**
 * @file  Server.cpp
 * @brief  The server to reroute clients to other clients
 * @author Tyler Stoney
 * @date 10 Dec 2018
 */
#include <map>
#include <vector>
#include <fcntl.h>
#include <stdio.h> 
#include <sstream>
#include <signal.h>
#include <stdlib.h> 
#include <string.h> 
#include <unistd.h>
#include <iostream>
#include <sys/sem.h>
#include <sys/shm.h>
#include <sys/mman.h> 
#include <netinet/in.h>
#include <sys/socket.h> 
#include <unordered_map>

#define PORT 15120 
#define MAX_CLIENTS 10

void* shSOCK;
void* temp;
int myAddr;
int32_t ret;
char *data;
void* shmem;
void* used_ports_shm;

struct sockaddr_in address; 

unsigned long ret_temp_long;
char *data_temp_long = (char*)&ret_temp_long;

int32_t ret_temp_int;
char *data_temp_int = (char*)&ret_temp_int;

int new_port_loc;
int port_for_cli_udp;

int shm_id;

int* readers_amt; /*! How many readers there are currently */
int* writers_amt; /*! How many writers to shmem there are currently */
bool* is_writing; /*! Whether the shared memory is currently being written to */


int semid;
std::vector<struct sembuf> sem;

int ports[]     = {15120, 15121, 15122, 15123, 15124,
                   15125, 15126, 15127, 15128, 15129,
                   15130, 15131, 15132, 15133, 15134,
                   15135, 15136, 15137, 15138, 15139};

bool port_used[]= {false, false, false, false, false,
                   false, false, false, false, false,
                   false, false, false, false, false,
                   false, false, false, false, false};

std::string OK = "OK"; 
std::string FAIL = "FAILD"; 
std::string NOMORE = "NOMOR"; 
std::string EXIT = "EXIT"; 

/**
 * @brief      semaphore union
 */
union semun {
    int              val;    // Value for SETVAL 
    struct semid_ds *buf;    // Buffer for IPC_STAT, IPC_SET 
    unsigned short  *array;  // Array for GETALL, SETALL 
    struct seminfo  *__buf;  // Buffer for IPC_INFO
                             //   (Linux-specific) 
};

/**
 * @brief metadata for a client
 */
typedef struct tagCLIENT   // Note the C-style dec.
{
	char Name[20] ;
	struct sockaddr_in serverAddr ;
	struct timeval startTime ;
	// Time of most recent lookup
	struct timeval lastLookupTime ;  
} CLIENT_INFO ;

/**
 * @brief The directory for all clients, akin to a phonebook; will be in shared memory
 */
typedef struct tagDIR  {                              
	CLIENT_INFO clientInfo[MAX_CLIENTS] ;
	int numClients ; // Total Clients in System 
} DIR ;

void create_tcp_port(int &server_fd, int &opt, bool * port_used, int (&ports)[20], struct sockaddr_in &address);
int find_unused_port(bool * ports_used);
int get_user_addr(struct tagDIR* dir, std::string name);
std::string list(struct tagDIR* dir, int sockets[10]);
void close_tcp(struct tagDIR* dir, int sock_fd, int my_addr, bool (&ports_used)[20]);
std::vector<struct sembuf> create_sem_set(int &semid);
void test_lock_sem(int sem_id, int id);
void test_unlock_sem(int sem_id, int id);
void start_read(int sem_id); // Reader: sem 0, Writer: sem 1;
void start_write(int sem_id); // Reader: sem 0, Writer: sem 1;
void finish_read(int sem_id); // Reader: sem 0, Writer: sem 1;
void finish_write(int sem_id); // Reader: sem 0, Writer: sem 1;
void shut_down(int s);

int main(int argc, char const *argv[]) 
{ 
    signal (SIGINT, shut_down);
    sem = create_sem_set(semid);
	DIR* dir = new DIR();
	(*dir).numClients = 0;

	int sockets[10] = {0};

	int server_fd, new_socket, valread; 
	
	int opt = 1; 
	int addrlen = sizeof(address); 
	char buffer[1024] = {0}; 
	

	pid_t pid;
	int status;

	key_t mem_key = ftok(".", getuid());
    shm_id = shmget(mem_key, (sizeof *dir), IPC_CREAT | 0666);

    if ( (shmem = shmat(shm_id, NULL, 0)) == (void*)-1 )
        perror("shmat: ");
	memcpy(shmem, &(*dir), sizeof(*dir));

    // Copy sockets dir to shared memory
	shSOCK = mmap(NULL, 320, (PROT_READ | PROT_WRITE), (MAP_ANONYMOUS | MAP_SHARED), 0, 0);
	memcpy(shSOCK, &(sockets), sizeof(sockets));

    used_ports_shm = mmap(NULL, sizeof(port_used), (PROT_READ | PROT_WRITE), (MAP_ANONYMOUS | MAP_SHARED), 0, 0);
    memcpy(used_ports_shm, &(port_used), sizeof(port_used));

    readers_amt = (int*)mmap(NULL, sizeof(int), (PROT_READ | PROT_WRITE), (MAP_ANONYMOUS | MAP_SHARED), 0, 0);
    writers_amt = (int*)mmap(NULL, sizeof(int), (PROT_READ | PROT_WRITE), (MAP_ANONYMOUS | MAP_SHARED), 0, 0);
    is_writing = (bool*)mmap(NULL, sizeof(bool), (PROT_READ | PROT_WRITE), (MAP_ANONYMOUS | MAP_SHARED), 0, 0);
    (*writers_amt) = 0;
    (*readers_amt) = 0;
    (*is_writing) = false;

	create_tcp_port(server_fd, opt, (bool*)used_ports_shm, ports, address);
	
	std::cout << "Waiting to accept first client..." << std::endl;
	while (1) {
		
		if ( (new_socket = accept(server_fd, 
								(struct sockaddr *)&address,
								(socklen_t*)&addrlen))>=0 ) 
		{ 	

			if ( (pid = fork()) < 0 ) {
		    	printf("*** ERROR: forking child process failed\n");
		        exit(1);
			} else if ( pid == 0 ) {      // CHILD : Handles each client
                //test_lock_sem(semid);
                start_read(semid);
                if ( static_cast<DIR *>(shmem)->numClients >= 10 ) {
                    send(new_socket, NOMORE.c_str(), 5, 0);
                    continue;
                } else {
                    send(new_socket, OK.c_str(), 2, 0);
                }
                finish_read(semid);
                static_cast<DIR *>(shmem)->clientInfo[static_cast<DIR *>(shmem)->numClients].serverAddr = address;
                valread = read( new_socket , buffer, 1024); 
                std::string input(buffer, valread);
                new_port_loc = find_unused_port((bool *)used_ports_shm);
                port_for_cli_udp = ports[new_port_loc];
                while(input=="NWUSR") {
                    printf("Ready to accept new user, new_socket: %d\n", new_socket);
                    send(new_socket , OK.c_str() , OK.length() , 0 ); 
                    memset(&buffer, 0, sizeof(buffer));
                    valread = read( new_socket , buffer, 1024); 
                    input.resize(valread);
                    input.assign(buffer);

                    // Check for username's existence
                    if ( get_user_addr(static_cast<DIR *>(shmem), input) == -1 ) {
                        // Send an OK to let the client know they 
                        //   have been accepted.
                        send(new_socket , OK.c_str() , OK.length() , 0 ); 
                        usleep(1500);
                        
                        send(new_socket, &port_for_cli_udp, sizeof(port_for_cli_udp), 0);

                        // Get new client's UDP file descriptor, store into appropriate spot in shared memory
                        valread = read(new_socket, data_temp_int, sizeof(ret_temp_int));
                        start_write(semid);
                        static_cast<int *>(shSOCK)[static_cast<DIR *>(shmem)->numClients] = new_socket;
                        finish_write(semid);

                        // Get new client's address
                        while( (valread = read(new_socket, data_temp_long, sizeof(ret_temp_long)) > 0) ){
                            break;
                        }

                        // Store values into memory
                        start_write(semid);
                        static_cast<DIR *>(shmem)->clientInfo[static_cast<DIR *>(shmem)->numClients].serverAddr.sin_addr.s_addr = (*(static_cast<unsigned long *>((void*)data_temp_long)));
                        static_cast<DIR *>(shmem)->clientInfo[static_cast<DIR *>(shmem)->numClients].serverAddr.sin_port = htons((unsigned short)port_for_cli_udp);
                        strcpy(static_cast<DIR *>(shmem)->clientInfo[static_cast<DIR *>(shmem)->numClients].Name, input.c_str());
                                             
                        static_cast<DIR *>(shmem)->numClients++; // Increase number of clients currently connected
                        printf("numClients: %d\n", static_cast<DIR *>(shmem)->numClients);
                        finish_write(semid);
                        
                        break;
                    } else {
                        printf("Can't accept user, name taken.\n");
                        ((bool*)used_ports_shm)[new_port_loc] = false;
                        send(new_socket , FAIL.c_str() , FAIL.length() , 0 ); 
                        break;
                    }
                }
				myAddr = static_cast<DIR *>(shmem)->numClients-1;
				while(1) {

					printf("Blocking in child on socket: %d\n", new_socket);

					valread = read( new_socket , buffer, 1024); 
					std::string input(buffer, valread);

					std::vector<std::string> result;
					std::istringstream iss(input);
					for(std::string s; iss >> s;) {
					    result.push_back(s);
					}
					if ( result[0] == "ALL" ) {
						std::cerr << "Listing all people on server...\n";
						std::string the_list = list(static_cast<DIR *>(shmem), static_cast<int *>(shSOCK));
						send(new_socket, the_list.c_str(), the_list.length(), 0);
                        continue;
					}
					else if ( result[0] == "EXIT") {
						send(new_socket, EXIT.c_str(), EXIT.length(), 0);
						close_tcp(static_cast<DIR *>(shmem), new_socket, myAddr, port_used);
						exit(0);
					}
					int addr = 0;

					// Notes: 
					//	new_socket: the socket of the client managed by this child
					//	shSOCK[addr]: the socket of the client this child's client
					//					wants to send to.
					start_read(semid);
                    if ( (addr = get_user_addr(static_cast<DIR *>(shmem), result[0])) != -1 ) {

						// Found the desired recipient's socket, send a notification to
						//   our client to wait for the socket data.
						std::cout << "Sending to " << result[0] << " on socket " << static_cast<int *>(shSOCK)[addr] << std::endl;
						send(new_socket, "SHMEM", 5, 0);
						usleep(1000);
						std::cerr << static_cast<DIR *>(shmem)->clientInfo[myAddr].serverAddr.sin_port << std::endl;
						
						std::cerr << static_cast<DIR *>(shmem)->clientInfo[addr].serverAddr.sin_addr.s_addr << std::endl;
                        send(new_socket,
								&(static_cast<DIR *>(shmem)->clientInfo[addr].serverAddr),
								sizeof(sockaddr_in), 0);

						//usleep(2000);

						// Send the recipient's socket data to our client.
						//send(new_socket, &static_cast<int *>(shSOCK)[addr], sizeof(static_cast<int *>(shSOCK)[addr]), 0);
						
					} else {
                        send(new_socket, FAIL.c_str(), FAIL.length(), 0);
						printf("User doesn't exist.\n");
					}
                    finish_read(semid);
		
				} // END WHILE
			} 

		} else {
			perror("accept"); 
            //shutdown(1);
			exit(EXIT_FAILURE);
		}
    
	} 

	
	return 0; 
} 

/**
 * @brief      Creates a tcp port.
 *
 * @param      server_fd  The server fd
 * @param      opt        The option
 * @param      port_used  The port used
 * @param      ports      The ports
 * @param      address    The address
 */
void create_tcp_port(int &server_fd, int &opt, bool * port_used, int (&ports)[20], struct sockaddr_in &address)
{
	// Creating socket stream file descriptor 
	if ( (server_fd = socket(AF_INET, SOCK_STREAM, 0)) == 0 ) { 
		perror("socket failed"); 
		exit(EXIT_FAILURE); 
	} 
	
	if ( setsockopt(server_fd, SOL_SOCKET,
					SO_REUSEADDR | SO_REUSEPORT,
					&opt, sizeof(opt)) ) 
	{ 
		perror("setsockopt"); 
		exit(EXIT_FAILURE); 
	} 
	address.sin_family = AF_INET; 
	address.sin_addr.s_addr = INADDR_ANY; 

	int next_port = find_unused_port(port_used);
	if ( next_port == -1 ){
		printf("No open ports.\n");
		exit(1);
	}
	address.sin_port = htons( ports[next_port] ); 

	if ( bind(server_fd, (struct sockaddr *)&address, sizeof(address))<0 ) 
	{ 
		perror("bind failed"); 
		exit(EXIT_FAILURE); 
	} 

	if ( listen(server_fd, 10) < 0 ) 
	{ 
		perror("listen"); 
		exit(EXIT_FAILURE); 
	} 
}

/**
 * @brief      finds the first unused port
 *
 * @param      ports_used  array of ports already in use
 *
 * @return     Returns the index of the first ununsed port
 */
int find_unused_port(bool * ports_used)
{
	for(int i = 0; i < 20; i++) {
		if ( ports_used[i] == false ) {
			ports_used[i] = true;
			return i;
		}
	}
	return -1;
} 

/**
 * @brief      Gets the user address.
 *
 * @param      dir   The dir
 * @param[in]  name  The name
 *
 * @return     The index of the user address.
 */
int get_user_addr(struct tagDIR* dir, std::string name) 
{	
	for(int i = 0; i < dir->numClients; i++){
		std::string u(dir->clientInfo[i].Name, name.length());
		std::cerr << dir->clientInfo[i].Name << std::endl;
		printf("U -> %s : name -> %s\n", dir->clientInfo[i].Name, name.c_str() );
		if(u == name) {
			return i;
		}
	}
	return -1;
}

/**
 * @brief      generates a list of clients currently connected
 *
 * @param      dir      The dir
 * @param      sockets  The sockets
 *
 * @return     A stringified list of connected clients.
 */
std::string list(struct tagDIR* dir, int sockets[10]) {
    std::string the_list = "All users online\n----------------\n";
	std::cerr << "USER \tSOCKET\tPORT" << std::endl;
	for(int i = 0; i < dir->numClients; i++){
        the_list = the_list+dir->clientInfo[i].Name+"\n";
		std::cerr << dir->clientInfo[i].Name << "\t" << sockets[i] << " \t" << dir->clientInfo[i].serverAddr.sin_port << std::endl;
    }
    return the_list;
}

/**
 * @brief      The shutdown process
 *
 * @param      dir         The dir
 * @param[in]  sock_fd     The sock fd
 * @param[in]  my_addr     My address
 * @param      ports_used  The ports used
 */
void close_tcp(struct tagDIR* dir, int sock_fd, int my_addr, bool (&ports_used)[20]) {
	for(int i = my_addr; i <= dir->numClients; i++) {
		dir->clientInfo[i] = dir->clientInfo[i+1];
	}
	dir->numClients--;
	ports_used[my_addr] = false;
	close(sock_fd);
}

/**
 * @brief      Creates a sem set.
 *
 * @param      semid  The semid
 *
 * @return     A vector containing 
 */
std::vector<struct sembuf> create_sem_set(int &semid)
{
    /*
    union semun {
        int              val;    // Value for SETVAL 
        struct semid_ds *buf;    // Buffer for IPC_STAT, IPC_SET 
        unsigned short  *array;  // Array for GETALL, SETALL 
        struct seminfo  *__buf;  // Buffer for IPC_INFO
                                 //   (Linux-specific) 
    };*/

    /*
    struct semid_ds {
        struct ipc_perm sem_perm;   // Ownership and permissions 
        time_t          sem_otime;  // Last semop time 
        time_t          sem_ctime;  // Last change time 
        unsigned short  sem_nsems;  // No. of semaphores in set 
    };*/

    struct sembuf sb[2];
    key_t semkey;
    if ( (semkey = ftok(".", getuid())) == (key_t) -1 ) {
        perror("ftok: ");
        exit(1);
    }
    
    /* Get semaphore ID associated with this key. */
    if ( (semid = semget(semkey, 0, 0)) == -1 ) {

        /* Semaphore does not exist - Create. */
        if ((semid = semget(semkey, 2, IPC_CREAT | IPC_EXCL | S_IRUSR |
            S_IWUSR | S_IRGRP | S_IWGRP | S_IROTH | S_IWOTH)) != -1)
        {
            for (int i = 0; i < 2; i++) {
            /* Initialize the semaphore. */
                sb[i].sem_num = i;
                sb[i].sem_op = 1;  
                sb[i].sem_flg = 0;
            }
            if (semop(semid, sb, 1) == -1) {
                perror("semop: ");
                exit(1);
            }
        }
        else if (errno == EEXIST) {
            if ( (semid = semget(semkey, 0, 0)) == -1 ) {
                perror("semget: ");
                exit(1);
            }
        }
        else {
            perror("semget");
            exit(1);
        }
    }   
    std::vector<struct sembuf> sbuf;
    sbuf.push_back(sb[0]);
    sbuf.push_back(sb[1]);
    return sbuf;
}

/**
 * @brief      locks the semaphore
 *
 * @param[in]  sem_id  The sem identifier
 * @param[in]  id      The sem identifier
 */
void test_lock_sem(int sem_id, int id)
{
    struct sembuf sem_lock = { (short unsigned int)id, -1, IPC_NOWAIT };
    if ( (semop(sem_id, &sem_lock, 1) == -1) )
       perror("semop");
}

/**
 * @brief      unlocks the semaphore
 *
 * @param[in]  sem_id  The sem identifier
 * @param[in]  id      The sem identifier
 */
void test_unlock_sem(int sem_id, int id)
{
    struct sembuf sem_unlock = { (short unsigned int)id, 1, IPC_NOWAIT };
    if ( (semop(sem_id, &sem_unlock, 1) == -1) )
       perror("semop");
}

/**
 * @brief      "Graceful" shutdown
 *
 * @param[in]  s     the signal sent (through CTRL+C)
 */
void shut_down(int s)
{
    int num = 0;
    int online_clients[10];
    for(int i = 0; i < 20; i++) {
        if ( ((bool*)used_ports_shm)[i] ) {
            online_clients[num++] = i;
        }
    }
    for(int i = 0; i <= 20; i++) {
        send(static_cast<int *>(shSOCK)[i], EXIT.c_str(), EXIT.length(), 0);
    }
    if( shmdt((const void*)shmem) < 0 )
        perror("shmdt: ");
    
    exit(1);
}

/**
 * @brief      adds to readers for readers/writers
 *
 * @param[in]  sem_id  The sem identifier
 */
void start_read(int sem_id) {
    if (*is_writing) {
        test_lock_sem(sem_id, 1);
    }
    (*readers_amt)++;
}

/**
 * @brief      notifies that there is a writer for readers/writers
 *
 * @param[in]  sem_id  The sem identifier
 */
void start_write(int sem_id) {
    (*writers_amt)++;
    if ( *writers_amt > 1 || (*readers_amt) > 0 ) {
        test_lock_sem(sem_id, 1);
    }
    (*is_writing) = true;
}

/**
 * @brief      Finishes a read.
 *
 * @param[in]  sem_id  The sem identifier
 */
void finish_read(int sem_id) {
    (*readers_amt)--;
    if ( (*readers_amt) == 0 ) {
        test_unlock_sem(sem_id, 1);
    }
}

/**
 * @brief      Signals that the writer is done
 *
 * @param[in]  sem_id  The sem identifier
 */
void finish_write(int sem_id) {
    (*writers_amt)--;
    test_unlock_sem(sem_id, 1);
    (*is_writing) = false;
}