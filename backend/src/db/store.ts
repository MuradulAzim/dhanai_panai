export interface ApiUser {
  apiKey: string;
  name: string;
  contactNumber: string;
  assignedNumber: string;
  smsBalance: number;
  callMinutesBalance: number;
  status: 'ACTIVE' | 'SUSPENDED';
  createdAt: string;
  totalSmsSent: number;
  totalCallMinutesUsed: number;
}

export interface RechargeRequest {
  id: string;
  userName: string;
  userContact: string;
  smsRequested: number;
  minutesRequested: number;
  totalAmountBdt: number;
  paymentMethod: 'BKASH' | 'NAGAD' | 'ROCKET';
  senderNumber: string;
  transactionId: string;
  screenshotBase64?: string;
  status: 'PENDING' | 'APPROVED' | 'REJECTED';
  assignedApiKey?: string;
  assignedPhoneNumber?: string;
  createdAt: string;
  reviewedAt?: string;
  adminNote?: string;
}

export interface RatesAndBanking {
  pricePerSmsBdt: number;
  pricePerCallMinuteBdt: number;
  bkashNumber: string;
  nagadNumber: string;
  rocketNumber: string;
  adminPin: string;
}

export interface MessageRecord {
  messageSid: string;
  apiKey?: string;
  conversationNumber: string;
  from: string;
  to: string;
  body: string;
  direction: 'INCOMING' | 'OUTGOING';
  status: 'SENDING' | 'SENT' | 'DELIVERED' | 'READ' | 'FAILED' | 'RECEIVED';
  createdAt: string;
}

export interface CallRecord {
  callSid: string;
  apiKey?: string;
  from: string;
  to: string;
  direction: 'INCOMING' | 'OUTGOING' | 'MISSED';
  status: 'RINGING' | 'IN_PROGRESS' | 'COMPLETED' | 'FAILED' | 'BUSY' | 'NO_ANSWER';
  duration: number;
  createdAt: string;
}

export const resellerStore = {
  ratesAndBanking: {
    pricePerSmsBdt: 2.0,
    pricePerCallMinuteBdt: 5.0,
    bkashNumber: '018XXXXXXXX',
    nagadNumber: '017XXXXXXXX',
    rocketNumber: '019XXXXXXXX',
    adminPin: '7788'
  } as RatesAndBanking,

  users: new Map<string, ApiUser>([
    [
      'sec_live_admin_demo',
      {
        apiKey: 'sec_live_admin_demo',
        name: 'Default Master Account',
        contactNumber: '+8801900000000',
        assignedNumber: '+18005550199',
        smsBalance: 9999,
        callMinutesBalance: 9999,
        status: 'ACTIVE',
        createdAt: new Date().toISOString(),
        totalSmsSent: 0,
        totalCallMinutesUsed: 0
      }
    ]
  ]),

  requests: [] as RechargeRequest[],

  messages: [] as MessageRecord[],

  calls: [] as CallRecord[]
};
