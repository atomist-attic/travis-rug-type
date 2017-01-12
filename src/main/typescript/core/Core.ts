import {TreeNode} from '@atomist/rug/tree/PathExpression'
import {Services} from '@atomist/rug/model/Core'

export interface Travis extends TreeNode {

  encrypt(repo: string, token: string, org: string, content: string): void

  enable(repo: string, token: string, org: string): void

  disable(repo: string, token: string, org: string): void
}

export interface TravisService extends Services {

  restartBuild(token: string, org: string, buildId: number): Status

}

export interface Status {

  success(): boolean

  message(): string

}