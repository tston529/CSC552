/**
 * @file  Client.cpp
 * @brief  Each person's terminal to chat with another person
 * @author Tyler Stoney
 * @date 10 Dec 2018
 */
#include <pwd.h>
#include <cctype>
#include <vector>
#include <errno.h>
#include <fcntl.h>
#include <netdb.h>
#include <sstream>
#include <stdio.h> 
#include <iostream>
#include <signal.h>
#include <stdlib.h> 
#include <string.h> 
#include <unistd.h>
#include <sys/ipc.h>
#include <sys/shm.h>
#include <sys/sem.h>
#include <sys/mman.h> 
#include <sys/stat.h>
#include <arpa/inet.h>
#include <sys/types.h>
#include <netinet/in.h>
#include <sys/socket.h> 
#include <initializer_list>

char thishostname[32];

#define PORT 15120 
#define MAX_CLIENTS 10

#define GREEN "\033[92m"
#define BLUE "\033[36m"
#define NOCOLOR "\033[0m"

std::vector<std::string> keywords = {"EXIT", "LIST", "ALL"};

void* senderData;
void* receiverData;

void *addr;
char *ipver;
struct sockaddr_in *ipv4;

void* shmem;

int semset;

struct sockaddr_in temp_addr;
char* temp_sock_addr = (char*)&temp_addr;
char ipstr[INET6_ADDRSTRLEN];

int server_sock = 0, valread; 
int udp_fd = 0;

struct sockaddr_in address; 
std::string hostname = "acad.kutztown.edu";

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
typedef struct tagLOCAL_INFO
{
    char name[20] ;
    struct timeval startTime;
    struct timeval lastMsgTime;
    int numMsg;
    pid_t pid ;
} LOCAL_INFO ;

/**
 * @brief The directory for that particular machine, will be in shared memory
 */
typedef struct tagLOCAL_DIR
{
    LOCAL_INFO  localInfo[MAX_CLIENTS];
    int numClients;
    int totalMsgs;
} LOCAL_DIR ;

/**
 * @brief Data per message, used in pretty-printing
 */
typedef struct tagMESSAGE
{
    std::string sender;
    std::string message;
    bool outgoing;
} MSG ;

std::vector<MSG> *messages;

void* address_to_ip_str(std::string hostname);
std::vector<struct sembuf> create_sem_set(int &semid);
int log_in(int fd, int argc, const char ** argv, std::string &username);
int make_udp_ear(std::string this_computer, std::string &tmp_ip, struct sockaddr_in &address, int port_from_server, unsigned long &temp_ip);
std::string to_upper(std::string s);
bool is_in(std::string s, std::vector<std::string> *arr);
void print_messages(std::vector<MSG> *v);
void test_lock_sem(std::vector<struct sembuf> &s, int sem_id);
void test_unlock_sem(std::vector<struct sembuf> &s, int sem_id);
void list();
void shut_down(int s);

int main(int argc, char const *argv[]) 
{ 
    if ( argc > 1 ) {
        std::string n(argv[1]);
        if ( is_in(to_upper(n), &(keywords)) ) {
            std::cerr << "Can't use a keyword as a username.\n";
            exit(1);
        }
    }
    // PUT CLIENTS INTO SHARED MEMORY
    LOCAL_DIR *locDIR = new LOCAL_DIR();

    //key_t mem_key = ftok("/tston529", getuid()+1);
    int shm_id = shmget(9000, (sizeof *locDIR), 0666);
    int er = errno;
    if ( (er = errno) == ENOENT ) {
        shm_id = shmget(9000, (sizeof *locDIR), IPC_CREAT | IPC_EXCL | 0666);
    }

    if ( (shmem = shmat(shm_id, NULL, 0)) == (void*)-1 )
        perror("shmat: ");
    
    if ( er == ENOENT ) {
        memcpy(shmem, &(*locDIR), sizeof(*locDIR));
        static_cast<LOCAL_DIR *>(shmem)->numClients = 0;
        static_cast<LOCAL_DIR *>(shmem)->totalMsgs = 0;
    }
    /////////

    int semid;
    std::vector<struct sembuf> sem = create_sem_set(semid);

    gethostname(thishostname, 32);
    std::string this_computer(thishostname);

    std::string username;
    messages = new std::vector<MSG>();
    struct sockaddr_in serv_addr; 
    int port_from_server = 0;
    
    char tmpMsg[1024] = {0};
 
    pid_t pid;
    int status;

    char buffer[1024] = {0}; 
    if ( (server_sock = socket(AF_INET, SOCK_STREAM, 0)) < 0 )
    { 
        printf("\n Socket creation error \n"); 
        return -1; 
    } 

    // Ensure server address struct is empty
    memset(&serv_addr, '0', sizeof(serv_addr)); 

    char ipstr[INET6_ADDRSTRLEN];
    std::string tmp_ip((reinterpret_cast<char*>(address_to_ip_str(hostname))));
    strcpy(ipstr, tmp_ip.c_str());
    // GET ADDRESS INFO    

    serv_addr.sin_family = AF_INET; 
    serv_addr.sin_port = htons(PORT); 
    
    // Convert IPv4 addresses from text to binary form 
    if ( inet_pton(AF_INET, ipstr, &serv_addr.sin_addr) <= 0 ) //was 127.0.0.1
    { 
        printf("\nInvalid address/ Address not supported \n"); 
        return -1; 
    } 

    if ( connect(server_sock, (struct sockaddr *)&serv_addr, sizeof(serv_addr)) < 0 ) 
    { 
        printf("\nConnection Failed \n"); 
        return -1; 
    } 
    valread = read( server_sock , buffer, 1024);
    std::string response(buffer, valread);
    if ( response == "NOMOR" ) {
        std::cerr << "No more room on the server. Better luck next time, bub.\n";
        exit(1);
    }

    port_from_server = log_in(server_sock, argc, argv, username);

    unsigned long t_ip = address.sin_addr.s_addr;
    udp_fd = make_udp_ear(this_computer, tmp_ip, address, port_from_server, t_ip);
    

    static_cast<LOCAL_DIR *>(shmem)->numClients++;
    signal (SIGINT, shut_down);

    struct sockaddr_in sin;
    socklen_t len = sizeof(address);
    if ( getsockname(udp_fd, (struct sockaddr *)&address, &len) == -1 )
        perror("getsockname");
    else
        printf("port number %d\n", ntohs(address.sin_port));
    
    // Send relevant info back to server
    send(server_sock , &udp_fd, sizeof(udp_fd) , 0 ); 
    usleep(1000);
    send(server_sock, &(t_ip), sizeof(t_ip), 0);

    fd_set rfds;
    int retval;

    std::vector<int> connected;
    std::string user, msg;
    int max = (server_sock > udp_fd)? server_sock : udp_fd;

    // Clear the screen initially
    fputs("\033[2J\033[;H", stdout);

    while (1) {
        /* Watch stdin (fd 0) to see when it has input. */
        FD_ZERO(&rfds);
        FD_SET(0, &rfds);
        FD_SET(server_sock, &rfds);
        FD_SET(udp_fd, &rfds);
        
        
        std::cerr << "> ";
        select(max+1, &rfds, NULL, NULL, NULL);
        // Listen on STDIN
        if ( FD_ISSET(0, &rfds) ) {
            
            int len = read(0, buffer, 1024);
            user = std::string(buffer, len);

            MSG mess;
            mess.outgoing = true;

            std::vector<std::string> result;
            std::istringstream iss(user);
            for(std::string s; iss >> s; ){
                result.push_back(s);
            }
            mess.sender = "To " + result[0];
            if ( is_in(to_upper(result[0]), &(keywords)) ) {
                result[0] = to_upper(result[0]);
            } else {
                msg = "";
                for(int i = 1; i < result.size(); i++) {
                    msg = msg + result[i] + " ";
                }
                mess.message = msg;
                msg = username;
                for(int i = 1; i < result.size(); i++) {
                    msg = msg + " " + result[i];
                }
                (*messages).push_back(mess);
                print_messages(messages);
            }
            user = result[0];
            if ( result[0] == "LIST" )
                list();
            else {
                test_lock_sem(sem, semid);
                send(server_sock , user.c_str() , user.length() , 0 ); 
                usleep(1000);
                test_unlock_sem(sem, semid);
            }
        }

        // LISTEN ON UDP EAR
        if ( FD_ISSET(udp_fd, &rfds) ) {
            memset( buffer, 0x00, 1024 );
            int slen = sizeof((sockaddr*)&(address));
            int valread = 0;

            if ( (valread = recvfrom( udp_fd , buffer, 1024, 0, ((sockaddr*)&(address)), (socklen_t *)&slen)) < 0) {
                printf("Failed to receive message.\n");

            } else {
                std::string m(buffer, valread);
                std::vector<std::string> r;
                std::istringstream iss(m);
                for(std::string s; iss >> s; ){
                    r.push_back(s);
                }
                m = "";
                for(int i = 1; i < r.size(); i++) {
                    m = m + r[i] + " ";
                }

                MSG mess;
                mess.sender = r[0];
                mess.message = m;
                mess.outgoing = false;
                (*messages).push_back(mess);
                
                print_messages(messages);
            }
        }

        // LISTEN ON SERVER SOCKET
        if ( FD_ISSET(server_sock, &rfds) ) {
            memset( buffer, 0x00, 1024 );
            valread = read( server_sock , buffer, 1024);
            std::string response(buffer, valread);
            if ( response == "SHMEM" ) {

                // TODO: Replace with a read to client's shared memory loc
                valread = read(server_sock, temp_sock_addr, sizeof(sockaddr_in) );
                
                int32_t ret;
                
                usleep(1000);
            
                int slen = sizeof(*(static_cast<const sockaddr *>((void*)temp_sock_addr)));
                
                test_lock_sem(sem, semid);
                //test_lock_sem(sem, semid);
                if ( sendto(udp_fd, msg.c_str(), msg.length(), MSG_EOR, (static_cast<const sockaddr *>((void*)temp_sock_addr)), slen) < 0 ) {
                    printf("Failed to send.\n");
                }
                test_unlock_sem(sem, semid);
                //std::cerr << "message sent" << std::endl;
                
            } else if ( response == "EXIT" ) {
                shut_down(9000);
            } else if ( response == "FAILD" ) {
                std::cerr << "That user doesn't exist.\n";
                (*messages).pop_back();
            } else {
                fprintf(stderr, "%s", buffer);
            }

        }
        

    } // END WHILE


    return 0; 
} 


/**
 * @brief      gets the ip address from a domain name, converts to string 
 *
 * @param[in]  hostname  The hostname
 *
 * @return     the ip string as a void* ?
 */
void* address_to_ip_str(std::string hostname)
{
    // Following logic and code modified from "Beej's Guide to Network Programming"
    // http://beej.us/guide/bgnet/pdf/bgnet_USLetter.pdf
    // 
    // Dynamically finds/updates the ip based on host
    
    void* addr;
    struct addrinfo hints, *res, *p;
    int sts = 0;

    memset(&hints, 0, sizeof hints);

    if ( (sts = getaddrinfo(hostname.c_str(), NULL, &hints, &res)) != 0 ) {
        fprintf(stderr, "getaddrinfo: %s\n", gai_strerror(sts));
        return NULL;
    }
    
    for ( p = res; p != NULL; p = p->ai_next ) {
        
        // get the pointer to the address itself,
        if ( p->ai_family == AF_INET ) { // IPv4
            ipv4 = (struct sockaddr_in *)p->ai_addr;
            addr = &(ipv4->sin_addr);
        }
        // convert the IP to a string
        inet_ntop(p->ai_family, addr, ipstr, sizeof ipstr);
    }
    // End modified code from Beej's guide
    ///////////////////////////////////////
    return (void*)ipstr;
}

/**
 * @brief      Sends client data to server
 *
 * @param[in]  fd        server's tcp file descriptor
 * @param[in]  argc      count of command line arguments
 * @param      argv      command line arguments
 * @param      username  Will hold the username since passed by reference
 *
 * @return     returns the port to create a udp socket on
 */
int log_in(int fd, int argc, const char ** argv, std::string &username)
{

    int32_t ret_temp;
    char *data_temp = (char*)&ret_temp;
    std::string response;

    int port_from_server = -1;

    char buffer[1024] = {0}; 

    std::string new_user = "NWUSR";

    ////////////////////////
    // Log in (choose username)
    if ( argc == 1 ) {
        std::cout << "Enter desired username:\n> ";
        std::cin >> username;
    } else {
        username = std::string(argv[1]);
    }

    send(fd , new_user.c_str() , new_user.length() , 0 ); 
    valread = read( fd , buffer, 1024);
    response.resize(valread);
    response.assign(buffer);
    if ( response == "OK" )
        send(fd , username.c_str() , username.length() , 0 ); 
    else
        exit(1);

    printf("Username sent.\n"); 
    valread = read( fd , buffer, 1024);

    response.resize(valread);
    response.assign(buffer);
    if ( response=="OK" ) {
        while ((valread = read( fd , data_temp, sizeof(ret_temp))) > 0) {
            break;
        }
        
        port_from_server = *(static_cast<int *>((void*)data_temp));
        LOCAL_INFO l;
        l.pid = getpid();
        strcpy(l.name, username.c_str());
        static_cast<LOCAL_DIR*>(shmem)->localInfo[static_cast<LOCAL_DIR*>(shmem)->numClients] = l;
        static_cast<LOCAL_DIR*>(shmem)->numClients++;
        return port_from_server;
    }
    std::cout << "Username already taken." << std::endl;
    exit(1);
    
    // End username selection
    //////////////////////////
}

/**
 * @brief      Creates a UDP file descriptor and sends that data to the server
 *
 * @param[in]  this_computer     The machine's hostname
 * @param      tmp_ip            String to be converted to unsigned long
 * @param      address           The address
 * @param[in]  port_from_server  The port from server
 * @param      temp_ip           The unsigned long to hold the converted ip address
 *
 * @return     The udp file descriptor
 */
int make_udp_ear(std::string this_computer, std::string &tmp_ip, struct sockaddr_in &address, int port_from_server, unsigned long &temp_ip)
{
    int udp_fd;
    int opt = 1;

    char ipstr[INET6_ADDRSTRLEN];
    ////////////////////////////
    // Make Listening UDP Socket
    if ( (udp_fd = socket(AF_INET, SOCK_DGRAM, IPPROTO_UDP)) == 0 ) { 
        perror("socket failed"); 
        exit(EXIT_FAILURE); 
    } 
   
    if ( setsockopt(udp_fd, SOL_SOCKET,
                    SO_REUSEADDR | SO_REUSEPORT,
                    &opt, sizeof(opt)) ) 
    { 
        perror("setsockopt"); 
        exit(EXIT_FAILURE); 
    } 

    if ( this_computer == "kupapcsit01" ) {
        tmp_ip = "156.12.127.7";
    } else if ( this_computer == "csitrd" ) {
        tmp_ip = "156.12.127.18";
    } else {
        tmp_ip = "156.12.127.29";
    }
    strcpy(ipstr, tmp_ip.c_str());
    address.sin_family = AF_INET; 
    if ( inet_pton(AF_INET, ipstr, &address.sin_addr.s_addr) <= 0 ) //was 127.0.0.1
    { 
        printf("\nInvalid address/Address not supported \n"); 
        return -1; 
    } 
    temp_ip = address.sin_addr.s_addr;
    address.sin_addr.s_addr = INADDR_ANY; 

    int next_port = port_from_server;
    if ( next_port == -1 ){
        printf("No open ports.\n");
        exit(1);
    }
    address.sin_port = htons( port_from_server ); 

    // Forcefully attaching socket to the port 
    if ( bind(udp_fd, (struct sockaddr *)&address, sizeof(address)) < 0 ) 
    { 
        perror("bind failed"); 
        exit(EXIT_FAILURE); 
    } 
    // END UDP CREATION
    ////////////////////////
    return udp_fd;
}

/**
 * @brief      converts a string to all uppercase
 *
 * @param[in]  s     the string to convert
 *
 * @return     returns the string in uppercase
 */
std::string to_upper(std::string s)
{
    char rslt[s.length()];
    strcpy(rslt, s.c_str());
    int i = 0;
    while(rslt[i]) {
        rslt[i]=toupper(rslt[i]);
        ++i;
    }
    std::string ret(rslt);
    return ret;
}

/**
 * @brief      Looks for instance of a string in an array
 *
 * @param[in]  s     the string to search for
 * @param      arr   the array to search through
 *
 * @return     True if in, False otherwise.
 */
bool is_in(std::string s, std::vector<std::string> *arr)
{
    for(int i = 0; i < (*arr).size(); i++) {
        if ( (*arr)[i] == s ) {
            return true;
        }
    }
    return false;
}

/**
 * @brief      Prints messages formatted like a chat client
 *
 * @param      v     the vector holding messages (outgoing and received)
 */
void print_messages(std::vector<MSG> *v)
{
    //fputs("\033[2J\033[;H", stderr);
    fprintf(stderr, "\0331A\033100C\033[1J\033[;H");
    int sz = (*v).size();
    //std::cout << std::endl;
    int start = (sz < 10) ? 0 : sz - 10;
    int extra_lines = 10;
    std::string color = NOCOLOR;
    std::string spaces = "";
    bool make_green = true;
    for (int i = sz - 1; i >= start; i--) {
        color = NOCOLOR;
        spaces = "";
        if ( (*messages)[i].outgoing ) {
            color = BLUE;
            spaces = "\t\t\t";
        }
        else if ( !(*messages)[i].outgoing && make_green ) {
            color = GREEN;
            spaces = "";
            make_green = false;
        }
        fprintf(stderr, "%s%s%s: %s\033[0m\n\n", spaces.c_str(), color.c_str(), (*v)[i].sender.c_str(), (*v)[i].message.c_str());
        extra_lines--;
    }
    for (int i = 0; i < extra_lines; i++) {
        fprintf(stderr, "\n" );
    }
    fprintf(stderr, "----------------------------------------\n" );
}

/**
 * @brief      Supposed to create a sem set.
 *
 * @param      semid  The semid
 *
 * @return     returns a vector of semaphore data (unused)
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

    struct sembuf sb[1];
    key_t semkey;
    if ( (semkey = ftok("/tmp", getuid())) == (key_t) -1 ) {
        perror("ftok"); 
        exit(1);
    }
    //printf("SemKey: %d\n", semkey);
    
    /* Get semaphore ID associated with this key. */
    if ( (semid = semget(semkey, 0, 0)) == -1 ) {

        /* Semaphore does not exist - Create. */
        if ((semid = semget(semkey, 1, IPC_CREAT | IPC_EXCL | S_IRUSR |
            S_IWUSR | S_IRGRP | S_IWGRP | S_IROTH | S_IWOTH)) != -1)
        {
            for ( int i = 0; i < 1; i++ ) {
                /* Initialize the semaphore. */
                sb[i].sem_num = 0;
                sb[i].sem_op = 1;  /* This is the number of runs
                                     without queuing. */
                sb[i].sem_flg = 0;
            }
            if (semop(semid, sb, 1) == -1) {
                perror("semop"); 
                exit(1);
            }
        }
        else if (errno == EEXIST) {
            if ( (semid = semget(semkey, 0, 0)) == -1 ) {
                perror("semget"); 
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
    return sbuf;
}

/**
 * @brief      supposed to lock a semaphore
 *
 * @param      s       vector holding semaphore data (unused)
 * @param[in]  sem_id  id of the semaphore to lock
 */
void test_lock_sem(std::vector<struct sembuf> &s, int sem_id)
{
    //std::cerr << "Locking...?" << std::endl;
    struct sembuf sem_lock = { 0, -1, IPC_NOWAIT };
    while ( (semop(sem_id, &sem_lock, 1) == -1) );
}

/**
 * @brief      supposed to unlock a semaphore
 *
 * @param      s       vector holding semaphore data (unused)
 * @param[in]  sem_id  id of the semaphore to unlock
 */
void test_unlock_sem(std::vector<struct sembuf> &s, int sem_id)
{
    //std::cerr << "Unlocking...?" << std::endl;
    struct sembuf sem_unlock = { 0, 1, IPC_NOWAIT };
    if ( semop(sem_id, &sem_unlock, 1) <= 0)
        return;
        //perror("semop");
}

/**
 * @brief      supposed to list all online users on this machine.
 */
void list()
{
    int sz = static_cast<LOCAL_DIR *>(shmem)->numClients;
    std::cerr << "Users on this machine\n-------------------" << std::endl;
    for (int i = 0; i < sz; i++) {
        std::cerr << static_cast<LOCAL_DIR *>(shmem)->localInfo[i].name << std::endl;
    }
}

void shut_down(int s)
{
    if ( s == 9000 )
        send(server_sock, "EXIT", 4, 0);
    std::cerr << "Yippee Yahoo" << std::endl;
    close(server_sock);
    //while(1);
    delete messages;
    /*delete shmem;
    delete shMSG;*/
    exit(1);
}
