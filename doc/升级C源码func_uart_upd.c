
#include "include.h"
#include "func.h"
#include "func_update.h"


#define TRACE_EN                0

#if TRACE_EN
#define TRACE(...)              printf(__VA_ARGS__)
#else
#define TRACE(...)
#endif

#if UART_M_UPDATE||UART_S_UPDATE

void uart_isr_init(u8 uart_num);
void uart_upd_putchar(u8 ch);
bool uart_getchar(u8 uart_num, u8* ch);

extern u8 upd_buf[512] AT(.upd_buf);

typedef struct
{
    volatile u32* GPIOXDE;
    volatile u32* GPIOXPU;
    volatile u32* GPIOXDIR;
    volatile u32* GPIOXFEN;

}uart_gpio_sfr_t;

typedef struct
{
    u16 w_cnt;
    u16 r_cnt;
    u8 *rx_buf;
    u8 rxcmd_buf[64];
    u16 rx_len;
    u8 uart_num;
}uart_upd_t ;

extern uart_upd_t uart_upd;

const uart_gpio_sfr_t uart_gpio_sfr[4] =
{
    {&GPIOADE,&GPIOAPU,&GPIOADIR,&GPIOAFEN},
    {&GPIOBDE,&GPIOBPU,&GPIOBDIR,&GPIOBFEN},
    {&GPIOEDE,&GPIOEPU,&GPIOEDIR,&GPIOEFEN},
    {&GPIOFDE,&GPIOFPU,&GPIOFDIR,&GPIOFFEN},

};

AT(.com_text.uart_isr)
void uart_upd_input(u8 ch)
{
    uart_upd.rx_buf[uart_upd.w_cnt & uart_upd.rx_len] = ch;
    uart_upd.w_cnt++;
//    my_print_r(&ch,1);
}

void uart_upd_port_init(u8 uart_port)
{
    u8 rx_port_sel = uart_port&0x03;
    u8 tx_port_sel = (uart_port&0x0C>>2);
//    TRACE("rx_port_sel %x tx_port_sel %x RX_BIT %x  TX_BIT %x\n",rx_port_sel,tx_port_sel,UART_RX_BIT,UART_TX_BIT);

    if (UART_TX_BIT != UART_RX_BIT) {
        *uart_gpio_sfr[rx_port_sel].GPIOXDE |= UART_RX_BIT;
        *uart_gpio_sfr[rx_port_sel].GPIOXPU |= UART_RX_BIT;
        *uart_gpio_sfr[rx_port_sel].GPIOXDIR |= UART_RX_BIT;
        *uart_gpio_sfr[rx_port_sel].GPIOXFEN |= UART_RX_BIT;

        *uart_gpio_sfr[tx_port_sel].GPIOXDE |= UART_TX_BIT;
        *uart_gpio_sfr[tx_port_sel].GPIOXDIR &=~ UART_TX_BIT;
        *uart_gpio_sfr[tx_port_sel].GPIOXFEN |= UART_TX_BIT;
    } else {
        *uart_gpio_sfr[tx_port_sel].GPIOXDE |= UART_TX_BIT;
        *uart_gpio_sfr[tx_port_sel].GPIOXFEN |= UART_TX_BIT;
    }

}

void uart_upd_init(u32 uart_port_map,u32 uart_baud,u8 sys_clk)
{
    TRACE("uart_upd_init\n");
    u8 uart_port = uart_port_map & 0x0f;
    uart_port_map = uart_port_map &( ~0x0f);

    if(UART0CON & BIT(0)) {
        while (!(UART0CON & BIT(8)));
    }

    u32 uartxcon = BIT(5)|BIT(7)|BIT(4)|BIT(0);  //bit5 uart_inc , sel 24M div2 TO UART CLK
    if (UART_TX_BIT == UART_RX_BIT) {
        uartxcon |= BIT(6);
    }
    u16 uart_baud_val = (24000000/2 + uart_baud/2)/uart_baud - 1;  //时钟源x24m_div2 //uart_baud/2 四舍五入
    u32 uartxbaud =(u32) uart_baud_val<<16|uart_baud_val;


#if UPDATE_UART_SEL == UPDATE_UART2
        if(uart_port_map&(0xf<<4)) {
              FUNCMCON1 |= (0x0f<<4);
         }
         if(uart_port_map&(0xf<<8)) {
              FUNCMCON1 |= (0x0f<<8);
         }
          CLKGAT1 |= BIT(11);
          FUNCMCON1 |= (u32)uart_port_map;
          UART2CON = uartxcon;//RX_EN UART_EN
          UART2BAUD = uartxbaud;
#else
        if(uart_port_map&(0xf<<8)){
            FUNCMCON0 |= (0x0f<<8);
        }
        if(uart_port_map&(0xf<<12)){
            FUNCMCON0 |=(0x0f<<12);
        }
        if(uart_port_map&(0xf<<24)){
            FUNCMCON0 |=(0x0f<<24);
        }
        if(uart_port_map&(0xf<<28)){
            FUNCMCON0 |=(0x0f<<28);
        }

        FUNCMCON0 |= (u32)uart_port_map;
#if UPDATE_UART_SEL == UPDATE_UART1
        CLKGAT0 |= BIT(21);
        UART1CON = 0;
        UART1CON = uartxcon;
        UART1BAUD = uartxbaud;
#else
        CLKGAT0 |= BIT(10);//UART0_CLK_EN
        UART0CON = uartxcon;
        UART0BAUD = uartxbaud;
#endif
#endif // UPDATE_UART_SEL
        CLKCON1 |= BIT(14);
        if((UART_TX_BIT == UART_RX_BIT) && (UART_TX_BIT == UART_VUSB_BIT)) {
            PWRCON0 |= BIT(30);                             //Enable VUSB GPIO
            RTCCON &= ~BIT(6);                              //关充电复位
        } else {
            uart_upd_port_init(uart_port);
        }



}

void uart_upd_isr_init(void)
{
    printf("uart_upd_isr_init\n");
    uart_rx_buf_init(UPDATE_UART_SEL);
//    uart_isr_init(UPDATE_UART_SEL);
}

void upd_putchar(u8 ch)
{
    uart_upd_putchar(ch);
}


AT(.com_text.uart)
bool upd_getchar(u8* ch)
{
    return uart_getchar(UPDATE_UART_SEL,ch);

}
#endif //UART_M_UPDATE||UART_S_UPDATE


#if UART_S_UPDATE

u8 uart_upd_get(u8 *ch);

typedef struct
{
    u32 uart_upd_baud;
    u32 uart_port_sel;
    u8 rx_bit;
    u8 tx_bit;
    u8 uart_num;
}uart_upd_init_t;

uart_upd_init_t uart_upd_p_init = {
    .uart_upd_baud    = UART_UPD_BAUD,
    .uart_port_sel    = UART_UPD_PORT_SEL,
    .rx_bit           = UART_RX_BIT,
    .tx_bit           = UART_TX_BIT,
    .uart_num         = UPDATE_UART_SEL,
};

AT(.text.update)
void get_uart_upd_info(u8 *buf,u8 len)
{
    memcpy(buf,&uart_upd_p_init,len);
}

//中断获取
u8 uart_upd_isr_get (u8 *ch)
{
    return uart_upd_get(ch);

}

AT(.com_text.uart)
bool uart_upd_isr_do(u8* ch)
{
#if UPDATE_UART_SEL == UPDATE_UART0
    if(UART0CON & BIT(9)) {
        UART0CPND = BIT(9);
        *ch = UART0DATA;
        return true;
    }
#elif UPDATE_UART_SEL == UPDATE_UART1
    if(UART1CON & BIT(9)) {
        UART1CPND = BIT(9);
        *ch = UART1DATA;
        return true;
    }
#else
    if(UART2CON & BIT(9)) {
        UART2CPND = BIT(9);
        *ch = UART2DATA;
        return true;
    }
#endif // UPDATE_UART_SEL
    return false;
}

#endif // UART_S_UPDATE






#if UART_M_UPDATE

#define UPD_CMD_LEN   12

enum
{
    UPD_M_SEND_START_SIGN,
    UPD_M_RECIVE_CMD,
    UPD_M_DONE,
};
enum
{
    CMD_CHECK_UART_UPD = 0x01,
    CMD_SEND_DATA,
    CMD_READ_STATUS,
};

typedef struct __attribute__((packed))
{
    u16 sign;
    u8 cmd;
    u8 status;
    u32 addr;
    u32 len;//当为读取数据时，为读取数据的长度
    u16 crc;
    u16 rsv[2];
}uart_upd_m_rxcmd_t;

//总共12byte
typedef struct __attribute__((packed)) {
{
    u16 sign;
    u8 cmd;
    u8 status;
    u32 addr;
    u32 data_crc;//当为发送数据时，是发送数据对应的crc
    u16 crc;
    u16 rsv[2];
}uart_upd_m_txcmd_t;

typedef struct
{
    u8 step;
    uart_upd_m_rxcmd_t rxcmd;
    uart_upd_m_txcmd_t txcmd;
    u32 addr;

}uart_upd_m_t;
uart_upd_m_t uart_upd_m;

extern const char upd_filename[];
const u8 str_send_upd_start_sign[] = {'S','T','A','R','T','_','U','P','D','^','_','^'};
const u8 str_recive_success[] = {'R','E','C','E','I','V','E','S','T','A','R','T'};

void uart_m_upd_send(void * buf,u32 len)
{
    u8 *ptr=(u8 *)buf;
#if TRACE_EN
    my_print_r(ptr,len);
#endif // TRACE_EN
    for (u32 j=0;j<len;j++) {
        upd_putchar(ptr[j]);
    }
}
bool get_slave_rsp(void)
{
    u8 ch;
    static u8 cnt = 0;
    //不要在里面加打印，不然从机发送太快会导致接收缺数据
    if(upd_getchar(&ch)){
        if(ch == str_recive_success[cnt]){
            cnt++;
            if(sizeof(str_recive_success) == cnt){
                uart_upd_m.step = UPD_M_RECIVE_CMD;
                return true;
            }
        }else {
            cnt=0;
        }
    }
//     my_printf("%x_%x  ",cnt,ch);
    return false;
}
void func_uart_update_enter(void)
{
    u8 buf[UPD_CMD_LEN];
    u32 get_rsp_tick=tick_get();
    fs_lseek(0, SEEK_SET);
    memset(&uart_upd_m,0,sizeof(uart_upd_m));
    memcpy(buf,str_send_upd_start_sign,sizeof(str_send_upd_start_sign));
    while(1){
//        printf("send_upd_start_sign\n");
      //100ms发送一次start命令
        if(tick_check_expire(get_rsp_tick,100)) {
            uart_m_upd_send(buf,sizeof(str_send_upd_start_sign));
            get_rsp_tick=tick_get();
        }
        if(get_slave_rsp()){
            break;
        }
        WDT_CLR();
    }
}

AT(.comm_rodate)
const char str_recive_cmd[]="recive_cmd:";

AT(.com_text.uart)
void recive_cmd(u8 *buf,u16 len)
{
    TRACE(str_recive_cmd);
    u8 ch;
    u8 cnt = 0;
    while(cnt!=len) {
        if(upd_getchar(&ch)){
            if((ch == 0xAA) && (cnt == 0)){
                buf[cnt++] = ch;
            }else if((ch == 0x55) && (cnt == 1)){
                buf[cnt++] = ch;
            }else if(cnt>1){
                buf[cnt++] = ch;
            }
        }
        WDT_CLR();
    }
}

u32 uart_cal_calc_crc(void *buf,u32 len)
{
    u8 *ptr = (u8*)buf;
    u32 crc_sum = 0;
    for(u32 i = 0;i < len;i ++){
        crc_sum += ptr[i];
    }

    return crc_sum;
}


bool upd_send_data(uart_upd_m_rxcmd_t *rxcmd,uart_upd_m_txcmd_t *txcmd)
{
    delay_ms(5);//每次接收到cmd等待5ms的时间，等待从机准备完毕
    uint rlen=0;
    for(u32 len=0;len<rxcmd->len;len+=512)
    {
        if(uart_upd_m.addr!=rxcmd->addr){
            if (FR_OK != fs_lseek(rxcmd->addr >> 9, SEEK_SET)) {
                return false;
            }
        }
        uart_upd_m.addr = rxcmd->addr + len;

        if(FR_OK == fs_read(upd_buf, 512, &rlen)) {
//            my_print_r(upd_buf,rlen);
            txcmd->sign = 0x55AA;
            txcmd->cmd = rxcmd->cmd;
            txcmd->addr = rxcmd->addr+len;
            txcmd->data_crc = uart_cal_calc_crc(upd_buf,rlen);
            txcmd->crc = (u16)uart_cal_calc_crc(txcmd,sizeof(uart_upd_m_txcmd_t)-2);
            uart_m_upd_send(txcmd,sizeof(uart_upd_m_txcmd_t));
            uart_m_upd_send(upd_buf,rlen);
        }

    }
    return true;

}
void rsp_check_uart_mode(uart_upd_m_txcmd_t *txcmd)
{
    delay_ms(5);//每次接收到cmd等待5ms的时间，等待从机准备完毕
    memset(txcmd,0,sizeof(uart_upd_m_txcmd_t));
    txcmd->sign = 0x55AA;
    txcmd->cmd = CMD_CHECK_UART_UPD;
    txcmd->crc = (u16)uart_cal_calc_crc(txcmd,sizeof(uart_upd_m_txcmd_t)-2);
    uart_m_upd_send(txcmd,sizeof(uart_upd_m_txcmd_t));
}
void upd_read_status(uart_upd_m_rxcmd_t *rxcmd)
{
    TRACE(" status %x\n",rxcmd->status);
    if(rxcmd->status == 0xff) {
        TRACE("UPD_DONE\n");
        uart_upd_m.step = UPD_M_DONE;
    }
}
void func_uart_update_event(void)
{
    uart_upd_m_rxcmd_t *rxcmd = &uart_upd_m.rxcmd;
    uart_upd_m_txcmd_t *txcmd = &uart_upd_m.txcmd;
    u16 len = sizeof(uart_upd_m_rxcmd_t);
    while(uart_upd_m.step!=UPD_M_DONE) {
        WDT_CLR();
        recive_cmd((u8*)rxcmd,len);
#if TRACE_EN
        my_print_r(rxcmd,len);
#endif // TRACE_EN
        switch(rxcmd->cmd)
        {
            case CMD_CHECK_UART_UPD:
                rsp_check_uart_mode(txcmd);
                break;
            case CMD_SEND_DATA:
                upd_send_data(rxcmd,txcmd);
                break;
            case CMD_READ_STATUS:
                upd_read_status(rxcmd);
                break;
            default:
                break;
        }
    }
}
void func_uart_update_exit(void)
{

}
void func_uart_update(void)
{
    if(fs_open(upd_filename, FA_READ) == FR_OK){
        func_uart_update_enter();
        func_uart_update_event();
        func_uart_update_exit();
    }
}
#endif

