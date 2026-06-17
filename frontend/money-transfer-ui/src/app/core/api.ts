export const API = {
  AUTH: {
    LOGIN: '/api/v1/auth/login',
    REGISTER: '/api/v1/auth/register'
  },

  TRANSFERS: {
    CREATE: '/api/v1/transfers',
    HISTORY: '/api/v1/transfers/history',
    ROLLBACK: (id: string) => `/api/v1/transfers/${id}/rollback`,
  },

  ACCOUNTS: {
    DETAILS: (id: number) => `/api/v1/accounts/${id}`,
    BALANCE: (id: number) => `/api/v1/accounts/${id}/balance`,
    SEARCH: '/api/v1/accounts/search',
    MY_ACCOUNTS: '/api/v1/accounts/my-accounts'
  },

  ADMIN: {
    PENDING: '/api/v1/admin/accounts/pending',
    APPROVE: (id: number) => `/api/v1/admin/accounts/${id}/approve`,
    REJECT: (id: number) => `/api/v1/admin/accounts/${id}/reject`,

    DEPOSIT: '/api/v1/admin/accounts/deposit',
    TRANSACTIONS: '/api/v1/admin/transactions',
    ALL_ACCOUNTS: '/api/v1/admin/accounts',

    ROLLBACK_REQUESTS: '/api/v1/admin/transfers/rollback-requests',
    APPROVE_ROLLBACK: (transactionId: string) => `/api/v1/admin/rollbacks/${transactionId}/approve`,
    REJECT_ROLLBACK: (transactionId: string) => `/api/v1/admin/rollbacks/${transactionId}/reject`,

    SNOWFLAKE: '/api/v1/analytics/kpis'
  },

  REWARDS: {
    HISTORY: '/api/v1/rewards',
    TOTAL: '/api/v1/rewards/total',
    GROUP: '/api/v1/rewards/group',
    GROUP_TOTAL: '/api/v1/rewards/group/total'
  },

  LINKED: {
    LIST: '/api/v1/accounts/linked',
    LINKABLE: '/api/v1/accounts/linkable',
    LINK: (id: number) => `/api/v1/accounts/link/${id}`
  }
};
